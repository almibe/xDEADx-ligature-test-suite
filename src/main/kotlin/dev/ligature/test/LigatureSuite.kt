/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.ligature.test

import io.kotlintest.shouldBe
import io.kotlintest.specs.AbstractStringSpec
import kotlinx.coroutines.flow.toSet
import org.libraryweasel.ligature.*

fun createSpec(creationFunction: () -> LigatureStore): AbstractStringSpec.() -> Unit {
    val testCollection = CollectionName("test")

    return {
        "Create and close store" {
            val store = creationFunction()
            store.compute { tx ->
                tx.collections()
            }.toSet() shouldBe setOf()
            store.close()
        }

        "creating a new collection" {
            val store = creationFunction()
            store.write { tx ->
                tx.createCollection(testCollection)
            }
            store.compute { tx ->
                tx.collections()
            }.toSet() shouldBe setOf(testCollection)
            store.close()
        }

        "access and delete new collection" {
            val store = creationFunction()
            store.write { tx ->
                tx.createCollection(testCollection)
            }
            store.compute { tx ->
                tx.collections()
            }.toSet() shouldBe setOf(testCollection)
            store.write { tx ->
                tx.deleteCollection(testCollection)
                tx.deleteCollection(CollectionName("test2"))
            }
            store.compute { tx ->
                tx.collections()
            }.toSet() shouldBe setOf()
            store.close()
        }

        "new collections should be empty" {
            val store = creationFunction()
            store.write { tx ->
                tx.createCollection(testCollection)
            }
            store.compute { tx ->
                tx.allStatements(testCollection)
            }.toSet() shouldBe setOf()
            store.close()
        }

        "adding statements to collections" {
            val store = creationFunction()
            store.write { tx ->
                val ent1 = tx.newEntity(testCollection)
                val ent2 = tx.newEntity(testCollection)
                tx.addStatement(testCollection, Statement(ent1, a, ent2, default))
                tx.addStatement(testCollection, Statement(ent1, a, ent2, ent2))
            }
            store.compute { tx ->
                tx.allStatements(testCollection)
            }.toSet() shouldBe
                    setOf(Statement(Entity(1), a, Entity(2), default),
                           Statement(Entity(1), a, Entity(2), Entity(2)))
            store.close()
        }

        "removing statements from collections" {
            val store = creationFunction()
            store.write { tx ->
                val ent1 = tx.newEntity(testCollection)
                val ent2 = tx.newEntity(testCollection)
                val ent3 = tx.newEntity(testCollection)
                tx.addStatement(testCollection, Statement(ent1, a, ent2, default))
                tx.addStatement(testCollection, Statement(ent3, a, ent2, default))
                tx.removeStatement(testCollection, Statement(ent1, a, ent2, default))
            }
            store.compute { tx ->
                tx.allStatements(testCollection)
            }.toSet() shouldBe
                    setOf(Statement(Entity(3), a, Entity(2), default))
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
                    Statement(Entity(1), a, Entity(2), Entity(3)),
                    Statement(Entity(4), a, Entity(5), Entity(6)))
            store.close()
        }

        "matching against a non-existant collection" {
            TODO()
        }

        "matching statements in collections" {
            val store = creationFunction()
            lateinit var valjean: Entity
            lateinit var javert: Entity
            store.write { tx ->
                valjean = tx.newEntity(testCollection)
                javert = tx.newEntity(testCollection)
                tx.addStatement(testCollection, Statement(valjean, Predicate("nationality"), StringLiteral("French")))
                tx.addStatement(testCollection, Statement(valjean, Predicate("prisonNumber"), LongLiteral(24601)))
                tx.addStatement(testCollection, Statement(javert, Predicate("nationality"), StringLiteral("French")))
            }
            store.compute { tx ->
                tx.matchStatements(testCollection, null, null, StringLiteral("French"))
                        .toSet() shouldBe setOf(
                            Statement(valjean, Predicate("nationality"), StringLiteral("French")),
                            Statement(javert, Predicate("nationality"), StringLiteral("French"))
                )
                tx.matchStatements(testCollection, null, null, LongLiteral(24601))
                        .toSet() shouldBe setOf(
                            Statement(valjean, Predicate("prisonNumber"), LongLiteral(24601))
                )
                tx.matchStatements(testCollection, valjean)
                        .toSet() shouldBe setOf(
                            Statement(valjean, Predicate("nationality"), StringLiteral("French")),
                            Statement(valjean, Predicate("prisonNumber"), LongLiteral(24601))
                )
                tx.matchStatements(testCollection, javert, Predicate("nationality"), StringLiteral("French"), default)
                        .toSet() shouldBe setOf(
                            Statement(javert, Predicate("nationality"), StringLiteral("French"))
                )
                tx.matchStatements(testCollection, null, null, null, default)
                        .toSet() shouldBe setOf(
                            Statement(valjean, Predicate("nationality"), StringLiteral("French")),
                            Statement(valjean, Predicate("prisonNumber"), LongLiteral(24601)),
                            Statement(javert, Predicate("nationality"), StringLiteral("French"))
                )
            }
            store.close()
        }

        "matching statements with literals and ranges in collections" {
            val store = creationFunction()
            lateinit var valjean: Entity
            lateinit var javert: Entity
            lateinit var trout: Entity
            store.write { tx ->
                valjean = tx.newEntity(testCollection)
                javert = tx.newEntity(testCollection)
                trout = tx.newEntity(testCollection)
                tx.addStatement(testCollection, Statement(valjean, Predicate("nationality"), StringLiteral("French")))
                tx.addStatement(testCollection, Statement(valjean, Predicate("prisonNumber"), LongLiteral(24601)))
                tx.addStatement(testCollection, Statement(javert, Predicate("nationality"), StringLiteral("French")))
                tx.addStatement(testCollection, Statement(javert, Predicate("prisonNumber"), LongLiteral(24602)))
                tx.addStatement(testCollection, Statement(trout, Predicate("nationality"), StringLiteral("American")))
                tx.addStatement(testCollection, Statement(trout, Predicate("prisonNumber"), LongLiteral(24603)))
            }
            store.compute { tx ->
                tx.matchStatements(testCollection, null, null, StringLiteralRange("French", "German"))
                        .toSet() shouldBe setOf(
                            Statement(valjean, Predicate("nationality"), StringLiteral("French")),
                            Statement(javert, Predicate("nationality"), StringLiteral("French"))
                )
                tx.matchStatements(testCollection, null, null, LongLiteralRange(24601, 24603))
                        .toSet() shouldBe setOf(
                            Statement(valjean, Predicate("prisonNumber"), LongLiteral(24601)),
                            Statement(javert, Predicate("prisonNumber"), LongLiteral(24602))
                )
                tx.matchStatements(testCollection, valjean, null, LongLiteralRange(24601, 24603))
                        .toSet() shouldBe setOf(
                            Statement(valjean, Predicate("prisonNumber"), LongLiteral(24601))
                )
            }
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
