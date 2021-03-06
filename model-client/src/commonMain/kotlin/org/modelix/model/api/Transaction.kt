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

package org.modelix.model.api

abstract class Transaction(override val branch: IBranch) : ITransaction {

    override fun containsNode(nodeId: Long): Boolean {
        return tree.containsNode(nodeId)
    }

    override fun getConcept(nodeId: Long): IConcept? {
        return tree.getConcept(nodeId)
    }

    override fun getParent(nodeId: Long): Long {
        return tree.getParent(nodeId)
    }

    override fun getRole(nodeId: Long): String? {
        return tree.getRole(nodeId)
    }

    override fun getProperty(nodeId: Long, role: String): String? {
        return tree.getProperty(nodeId, role)
    }

    override fun getReferenceTarget(sourceId: Long, role: String): INodeReference? {
        return tree.getReferenceTarget(sourceId, role)
    }

    override fun getChildren(parentId: Long, role: String?): Iterable<Long> {
        return tree.getChildren(parentId, role)
    }

    override fun getAllChildren(parentId: Long): Iterable<Long> {
        return tree.getAllChildren(parentId)
    }
}
