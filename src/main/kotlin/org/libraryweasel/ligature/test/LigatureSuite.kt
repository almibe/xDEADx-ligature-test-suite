/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.libraryweasel.ligature.test

import io.kotlintest.shouldBe
import io.kotlintest.specs.AbstractStringSpec
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.toSet
import org.libraryweasel.ligature.*

fun createSpec(creationFunction: () -> LigatureStore): AbstractStringSpec.() -> Unit {
    val testCollection = CollectionName("test")

    return {
        "Create and close store" {
            val store = creationFunction()
            store.compute { tx ->
                tx.collections()
            }.toList() shouldBe listOf()
            store.close()
        }

        "creating a new collection" {
            val store = creationFunction()
            store.write { tx ->
                tx.createCollection(testCollection)
            }
            store.compute { tx ->
                tx.collections()
            }.toList() shouldBe listOf(testCollection)
            store.close()
        }

        "access and delete new collection" {
            val store = creationFunction()
            store.write { tx ->
                tx.createCollection(testCollection)
            }
            store.compute { tx ->
                tx.collections()
            }.toList() shouldBe listOf(testCollection)
            store.write { tx ->
                tx.deleteCollection(testCollection)
                tx.deleteCollection(CollectionName("test2"))
            }
            store.compute { tx ->
                tx.collections()
            }.toList() shouldBe listOf()
            store.close()
        }

        "new collections should be empty" {
            val store = creationFunction()
            store.write { tx ->
                tx.createCollection(testCollection)
            }
            store.compute { tx ->
                tx.allStatements(testCollection)
            }.toList() shouldBe listOf()
            store.close()
        }

        "adding statements to collections" {
            val store = creationFunction()
            store.write { tx ->
                tx.addStatement(testCollection, Statement(Entity("This"), a, Entity("test"), default))
                tx.addStatement(testCollection, Statement(Entity("This"), a, Entity("test"), Entity("test")))
            }
            store.compute { tx ->
                tx.allStatements(testCollection)
            }.toList() shouldBe
                    listOf(Statement(Entity("This"), a, Entity("test"), default),
                           Statement(Entity("This"), a, Entity("test"), Entity("test")))
            store.close()
        }

        "removing statements from collections" {
            val store = creationFunction()
            store.write { tx ->
                tx.addStatement(testCollection, Statement(Entity("This"), a, Entity("test"), default))
                tx.addStatement(testCollection, Statement(Entity("Also"), a, Entity("test"), default))
                tx.removeStatement(testCollection, Statement(Entity("This"), a, Entity("test"), default))
            }
            store.compute { tx ->
                tx.allStatements(testCollection)
            }.toList() shouldBe
                    listOf(Statement(Entity("Also"), a, Entity("test"), default))
            store.close()
        }

        "new entity test" {
            val store = creationFunction()
            store.write { tx ->
                tx.addStatement(testCollection, Statement(tx.newEntity(testCollection), a, tx.newEntity(testCollection), tx.newEntity(testCollection)))
                tx.addStatement(testCollection, Statement(tx.newEntity(testCollection), a, tx.newEntity(testCollection), tx.newEntity(testCollection)))
            }
            store. compute { tx ->
                tx.allStatements(testCollection)
            }.toSet() shouldBe setOf(
                    Statement(Entity("_:1"), a, Entity("_:2"), Entity("_:3")),
                    Statement(Entity("_:4"), a, Entity("_:5"), Entity("_:6")))
            store.close()
        }

        "matching statements in collections" {
            val store = creationFunction()
            store.write { tx ->
                tx.addStatement(testCollection, Statement(Entity("This"), a, Entity("test"), default))
                tx.addStatement(testCollection, Statement(tx.newEntity(testCollection), a, Entity("test"), default))
                tx.addStatement(testCollection, Statement(Entity("a"), Predicate("knows"), Entity("b"), default))
                tx.addStatement(testCollection, Statement(Entity("b"), Predicate("knows"), Entity("c"), default))
                tx.addStatement(testCollection, Statement(Entity("c"), Predicate("knows"), Entity("a"), default))
                tx.addStatement(testCollection, Statement(Entity("c"), Predicate("knows"), Entity("a"), default)) //dupe
                tx.addStatement(testCollection, Statement(tx.newEntity(testCollection), Predicate("fortyTwo"), tx.newEntity(testCollection), tx.newEntity(testCollection)))
            }
            store.compute { tx ->
                tx.matchStatements(testCollection).toSet().size shouldBe 6
                tx.matchStatements(testCollection, null, null, null, default).toSet().size shouldBe 5
                tx.matchStatements(testCollection, null, a, null).toSet() shouldBe setOf(
                        Statement(Entity("This"), a, Entity("test"), default),
                        Statement(Entity("_:1"), a, Entity("test"), default)
                )
                tx.matchStatements(testCollection, null, a, Entity("test")).toSet() shouldBe setOf(
                        Statement(Entity("This"), a, Entity("test"), default),
                        Statement(Entity("_:1"), a, Entity("test"), default)
                )
                tx.matchStatements(testCollection, null, null, Entity("test"), null).toSet() shouldBe setOf(
                        Statement(Entity("This"), a, Entity("test"), default),
                        Statement(Entity("_:1"), a, Entity("test"), default)
                )
                tx.matchStatements(testCollection, null, null, null, Entity("_:4")).toSet() shouldBe setOf(
                        Statement(Entity("_:2"), Predicate("fortyTwo"), Entity("_:3"), Entity("_:4"))
                )
            } // TODO add test running against a non-existant collection w/ match-statement calls
            store.close()
        }

        "matching statements with literals and ranges in collections" {
            val store = creationFunction()
            store.write { tx ->
                tx.addStatement(testCollection, Statement(Entity("This"), Predicate("test"), StringLiteral("aa"), default))
                tx.addStatement(testCollection, Statement(Entity("This"), Predicate("test"), StringLiteral("bb"), default))
                tx.addStatement(testCollection, Statement(Entity("This"), Predicate("test"), StringLiteral("cc"), default))
                tx.addStatement(testCollection, Statement(Entity("This"), Predicate("test"), StringLiteral("cd"), default))
                tx.addStatement(testCollection, Statement(tx.newEntity(testCollection), Predicate("test"), LangLiteral("le test", "fr"), default))
                tx.addStatement(testCollection, Statement(tx.newEntity(testCollection), Predicate("test"), LangLiteral("le test", "en"), default))
                tx.addStatement(testCollection, Statement(tx.newEntity(testCollection), Predicate("test"), LangLiteral("le test2", "fr"), default))
                tx.addStatement(testCollection, Statement(tx.newEntity(testCollection), Predicate("test"), LangLiteral("le test3", "fr"), default))
                tx.addStatement(testCollection, Statement(Entity("a"), Predicate("test"), LongLiteral(100L), default))
                tx.addStatement(testCollection, Statement(Entity("a"), Predicate("test2"), LongLiteral(100L), default))
                tx.addStatement(testCollection, Statement(Entity("b"), Predicate("test"), LongLiteral(1000L), default))
                tx.addStatement(testCollection, Statement(Entity("c"), Predicate("test"), DoubleLiteral(42.0), default))
                tx.addStatement(testCollection, Statement(Entity("c"), Predicate("test"), DoubleLiteral(42.0), default)) //dupe
                tx.addStatement(testCollection, Statement(Entity("c"), Predicate("test"), DoubleLiteral(2.0), default))
                tx.addStatement(testCollection, Statement(tx.newEntity(testCollection), a, tx.newEntity(testCollection), tx.newEntity(testCollection)))
            }
            store.compute { tx ->
                tx.matchStatements(testCollection).toSet().size shouldBe 14
                tx.matchStatements(testCollection, null, null, LongLiteral(100L), default).toSet().size shouldBe 2
                tx.matchStatements(testCollection, null, null, StringLiteralRange("b", "cc")).toSet() shouldBe setOf(
                        Statement(Entity("This"), Predicate("test"), StringLiteral("bb"), default),
                        Statement(Entity("This"), Predicate("test"), StringLiteral("cc"), default)
                )
                tx.matchStatements(testCollection, null, null, LangLiteralRange(LangLiteral("le test", "fr"), LangLiteral("le test2", "fr"))).toSet() shouldBe setOf(
                        Statement(Entity("_:1"), Predicate("test"), LangLiteral("le test", "fr"), default),
                        Statement(Entity("_:3"), Predicate("test"), LangLiteral("le test2", "fr"), default)
                )
                tx.matchStatements(testCollection, null, null, LongLiteralRange(99L, 100L), null).toSet() shouldBe setOf(
                        Statement(Entity("a"), Predicate("test"), LongLiteral(100L), default),
                        Statement(Entity("a"), Predicate("test2"), LongLiteral(100L), default)
                )
                tx.matchStatements(testCollection, null, null, DoubleLiteralRange(2.1, 42.0), default).toSet() shouldBe setOf(
                        Statement(Entity("c"), Predicate("test"), DoubleLiteral(42.0), default)
                )
            } // TODO add test running against a non-existant collection w/ match-statement calls
            store.close()
        }

//    "matching statements with collection literals in collections" {
//        val store = creationFunction()
//        val collection = store.createCollection(Entity("test"))
//        collection shouldNotBe null
//        val tx = collection.writeTx()
//        TODO("Add values")
//        tx.commit()
//        val tx = collection.tx()
//        TODO("Add assertions")
//        tx.cancel() // TODO add test running against a non-existant collection w/ match-statement calls
//    }
    }
}
