/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License. 
 */

package org.modelix.model

import org.modelix.model.api.IBranch
import org.modelix.model.api.IIdGenerator
import org.modelix.model.api.ITree
import org.modelix.model.api.PBranch
import org.modelix.model.lazy.CLTree
import org.modelix.model.lazy.CLVersion
import org.modelix.model.lazy.IDeserializingKeyValueStore
import org.modelix.model.operations.*
import org.modelix.model.persistent.CPVersion
import kotlin.math.max

class VersionMerger(private val storeCache: IDeserializingKeyValueStore, private val idGenerator: IIdGenerator) {
    private val mergeLock = Any()
    fun mergeChange(lastMergedVersion: CLVersion, newVersion: CLVersion): CLVersion {
        var lastMergedVersion = lastMergedVersion
        runSynchronized(mergeLock) {
            return if (lastMergedVersion == null) {
                lastMergedVersion = newVersion
                newVersion
            } else {
                if (newVersion.hash == lastMergedVersion.hash) {
                    return lastMergedVersion
                }
                val merged = mergeHistory(lastMergedVersion.hash, newVersion.hash)
                checkTreeIds(lastMergedVersion, newVersion)
                merged
            }
        }
    }

    fun checkTreeIds(v1: CLVersion, v2: CLVersion) {
        val id1 = v1.tree.id
        val id2 = v2.tree.id
        if (id1 != id2) {
            throw RuntimeException("Tree ID mismatch: $id1 and $id2")
        }
    }

    protected fun mergeHistory(leftVersionHash: String, rightVersionHash: String): CLVersion {
        val commonBase = commonBaseVersion(leftVersionHash, rightVersionHash)
        val leftHistory = getHistory(leftVersionHash, commonBase)
        val rightHistory = getHistory(rightVersionHash, commonBase)
        var mergedVersion: CLVersion? = null
        var tree = getVersion(commonBase)?.tree
        if (tree == null) {
            tree = CLTree(storeCache)
        }
        val branch: IBranch = PBranch(tree, idGenerator)
        if (rightHistory.isEmpty() || leftHistory.isEmpty()) {
            val fastForwardHistory = if (leftHistory.isEmpty()) rightHistory else leftHistory
            val numOps = fastForwardHistory.map { obj: CLVersion -> obj.numberOfOperations }.fold(0) { a: Int, b: Int -> max(a, b) }
            if (numOps > 100) {
                return fastForwardHistory[0]
            }
            // A small number of changes may be faster to compute locally. 
        }
        branch.runWrite {
            val t = branch.writeTransaction
            val appliedOpsForVersion: MutableMap<Long, List<IAppliedOperation>> = LinkedHashMap()
            val appliedVersionIds: MutableSet<Long> = LinkedHashSet()
            while (leftHistory.isNotEmpty() || rightHistory.isNotEmpty()) {
                val useLeft = when {
                    rightHistory.isEmpty() -> true
                    leftHistory.isEmpty() -> false
                    else -> leftHistory.last().id < rightHistory.last().id
                }
                val versionToApply = (if (useLeft) leftHistory else rightHistory).let { it.removeAt(it.size - 1) }
                if (appliedVersionIds.contains(versionToApply.id)) {
                    continue
                }
                appliedVersionIds.add(versionToApply.id)

                val operationsToApply = captureIntend(versionToApply)
                val appliedOps = operationsToApply.flatMap {
                    val transformed: List<IOperation>
                    try {
                        transformed = it.restoreIntend(t.tree)
                        logTrace(
                            {
                                if (transformed.size != 1 || transformed[0] != it.getOriginalOp())
                                    "transformed: ${it.getOriginalOp()} --> $transformed"
                                else ""
                            },
                            VersionMerger::class
                        )
                    } catch (ex: Exception) {
                        throw RuntimeException("Operation intend failed: ${it.getOriginalOp()}", ex)
                    }
                    transformed.map { o ->
                        try {
                            o.apply(t)
                        } catch (ex: Exception) {
                            throw RuntimeException("Operation failed: $o", ex)
                        }
                    }
                }
                appliedOpsForVersion[versionToApply.id] = appliedOps
                mergedVersion = CLVersion(
                    versionToApply.id,
                    versionToApply.time,
                    versionToApply.author,
                    (t.tree as CLTree).hash,
                    if (mergedVersion != null) mergedVersion!!.hash else versionToApply.previousHash,
                    versionToApply.data?.originalVersion ?: versionToApply.hash,
                    appliedOps.map { it.getOriginalOp() }.toTypedArray(),
                    storeCache
                )
            }
        }
        if (mergedVersion == null) {
            throw RuntimeException("Failed to merge $leftVersionHash and $rightVersionHash")
        }
        return mergedVersion!!
    }

    private fun captureIntend(version: CLVersion): List<IOperationIntend> {
        val tree = version.previousVersion!!.tree
        val branch = PBranch(tree, idGenerator)
        return branch.computeWrite {
            val a = version.operations.map {
                val intend = it.captureIntend(branch.transaction.tree)
                it.apply(branch.writeTransaction)
                intend
            }
            a
        }
    }

    /**
     *
     *
     * @param fromVersion The newest version
     * @param toVersionExclusive The oldest version
     * @return Newest version first
     */
    protected fun getHistory(fromVersion: String, toVersionExclusive: String?): MutableList<CLVersion> {
        val history: MutableList<CLVersion> = ArrayList()
        if (fromVersion == toVersionExclusive) {
            return history
        }
        var version = getVersion(fromVersion)
        while (true) {
            if (version == null) {
                break
            }
            history.add(version)
            if (version.previousHash == null) {
                break
            }
            if (version.previousHash == toVersionExclusive) {
                break
            }
            version = version.previousVersion
        }
        return history
    }

    protected fun commonBaseVersion(leftHash: String?, rightHash: String?): String? {
        var leftHash = leftHash
        var rightHash = rightHash
        val leftVersions: MutableSet<String> = HashSet()
        val rightVersions: MutableSet<String> = HashSet()
        while (leftHash != null || rightHash != null) {
            if (leftHash != null) {
                leftVersions.add(leftHash)
            }
            if (rightHash != null) {
                rightVersions.add(rightHash)
            }
            if (leftHash != null) {
                if (rightVersions.contains(leftHash)) {
                    return leftHash
                }
            }
            if (rightHash != null) {
                if (leftVersions.contains(rightHash)) {
                    return rightHash
                }
            }
            if (leftHash != null) {
                leftHash = getVersion(leftHash)?.previousHash
            }
            if (rightHash != null) {
                rightHash = getVersion(rightHash)?.previousHash
            }
        }
        return null
    }

    private fun getVersion(hash: String?): CLVersion? {
        return if (hash == null) {
            null
        } else CLVersion.loadFromHash(hash, storeCache)
    }

    protected fun getTree(version: CPVersion?): ITree? {
        return if (version == null) {
            null
        } else CLTree(version.treeHash, storeCache)
    }
}
