/*
 * Copyright 2020 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.xml

import org.junit.jupiter.api.Test
import org.openrewrite.ExecutionContext
import org.openrewrite.xml.tree.Xml

class RemoveContentTest : XmlRecipeTest {

    @Test
    fun removeContent() = assertChanged(
        recipe = toRecipe {
            object : XmlVisitor<ExecutionContext>() {
                override fun visitDocument(x: Xml.Document, p: ExecutionContext): Xml {
                    if (p.getMessage("cyclesThatResultedInChanges", 0) == 0) {
                        doAfterVisit(RemoveContentVisitor(x.root.content[1] as Xml.Tag, false))
                    }
                    return super.visitDocument(x, p)
                }
            }
        },
        before = """
            <dependency>
                <groupId>group</groupId>
                <version/>
            </dependency>
        """,
        after = """
            <dependency>
                <groupId>group</groupId>
            </dependency>
        """
    )

    @Test
    fun removeAncestorsThatBecomeEmpty() = assertChanged(
        recipe = toRecipe {
            object : XmlVisitor<ExecutionContext>() {
                override fun visitDocument(x: Xml.Document, p: ExecutionContext): Xml {
                    if (p.getMessage("cyclesThatResultedInChanges", 0) == 0) {
                        val groupId = x.root.children[1].children.first().children.first()
                        doAfterVisit(RemoveContentVisitor(groupId, true))
                    }
                    return super.visitDocument(x, p)
                }
            }
        },
        before = """
            <project>
                <name>my.company</name>
                <dependencyManagement>
                    <dependencies>
                        <groupId>group</groupId>
                    </dependencies>
                </dependencyManagement>
            </project>
        """,
        after = """
            <project>
                <name>my.company</name>
            </project>
        """
    )

    @Test
    fun rootChangedToEmptyTagIfLastRemainingTag() = assertChanged(
        recipe = toRecipe {
            object : XmlVisitor<ExecutionContext>() {
                override fun visitDocument(x: Xml.Document, p: ExecutionContext): Xml {
                    if (p.getMessage("cyclesThatResultedInChanges", 0) == 0) {
                        val groupId = x.root.children.first().children.first().children.first()
                        doAfterVisit(RemoveContentVisitor(groupId, true))
                    }
                    return super.visitDocument(x, p)
                }
            }
        },
        before = """
            <project>
                <dependencyManagement>
                    <dependencies>
                        <groupId>group</groupId>
                    </dependencies>
                </dependencyManagement>
            </project>
        """,
        after = """
            <project/>
        """
    )
}
