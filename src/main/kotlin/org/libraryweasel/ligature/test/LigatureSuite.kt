/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.libraryweasel.ligature.test

import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import io.kotlintest.specs.AbstractStringSpec
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.toSet
import org.libraryweasel.ligature.*

fun createSpec(creationFunction: () -> LigatureStore): AbstractStringSpec.() -> Unit {
    return {
        "Create and close store" {
            val store = creationFunction()
            val tx = creationFunction().readTx()
            tx.collections().toList() shouldBe listOf()
            tx.cancel()
            store.close()
        }

        "access new collection" {
            val store = creationFunction()
            val tx = store.readTx()
            tx.collection(CollectionName("test")) shouldBe null
            tx.collections().toList() shouldBe listOf()
            tx.cancel()
            store.close()
        }

        "creating a new collection" {
            val store = creationFunction()
            val tx = store.writeTx()
            tx.collection(CollectionName("test")) shouldBe null
            tx.collections().toList() shouldBe listOf(Entity("test"))
            tx.commit()
            store.close()
        }

        "access and delete new collection" {
            val store = creationFunction()
            val tx = store.writeTx()
            tx.collection(CollectionName("test")) shouldNotBe null
            tx.collections().toList() shouldBe listOf(CollectionName("test"))
            tx.deleteCollection(CollectionName("test"))
            tx.deleteCollection(CollectionName("test2"))
            tx.collections().toList() shouldBe listOf()
            tx.cancel()
            store.close()
        }

        "new collections should be empty" {
            val store = creationFunction()
            val tx = store.writeTx()
            val collection = tx.collection(CollectionName("test"))
            collection shouldNotBe null
            collection.allStatements().toList() shouldBe listOf()
            tx.cancel()
            store.close()
        }

        "adding statements to collections" {
            val store = creationFunction()
            val tx = store.writeTx()
            val collection = tx.collection(CollectionName("test"))
            collection shouldNotBe null
            collection.addStatement(Statement(Entity("This"), a, Entity("test"), default))
            tx.commit()
            val readTx = store.readTx()
            readTx.collection(CollectionName("test"))!!.allStatements().toList() shouldBe
                    listOf(Statement(Entity("This"), a, Entity("test"), default))
            readTx.cancel()
            store.close()
        }

        "removing statements from collections" {
            val store = creationFunction()
            val tx = store.writeTx()
            val collection = tx.collection(CollectionName("test"))
            collection shouldNotBe null
            collection.addStatement(Statement(Entity("This"), a, Entity("test"), default))
            collection.addStatement(Statement(Entity("Also"), a, Entity("test"), default))
            collection.removeStatement(Statement(Entity("This"), a, Entity("test"), default))
            tx.commit()
            val readTx = store.readTx()
            readTx.collection(CollectionName("test"))!!.allStatements().toList() shouldBe
                    listOf(Statement(Entity("Also"), a, Entity("test"), default))
            readTx.cancel()
            store.close()
        }

        "new entity test" {
            val store = creationFunction()
            val tx = store.writeTx()
            val collection = tx.collection(CollectionName("test"))
            collection shouldNotBe null
            collection.addStatement(Statement(collection.newEntity(), a, collection.newEntity(), collection.newEntity()))
            collection.addStatement(Statement(collection.newEntity(), a, collection.newEntity(), collection.newEntity()))
            tx.commit()
            val readTx = store.readTx()
            readTx.collection(CollectionName("test"))!!.allStatements().toSet() shouldBe setOf(
                    Statement(Entity("_:1"), a, Entity("_:2"), Entity("_:3")),
                    Statement(Entity("_:4"), a, Entity("_:5"), Entity("_:6")))
            readTx.cancel()
            store.close()
        }

        "matching statements in collections" {
            val store = creationFunction()
            val tx = store.writeTx()
            val collection = tx.collection(CollectionName("test"))
            collection shouldNotBe null
            collection.addStatement(Statement(Entity("This"), a, Entity("test"), default))
            collection.addStatement(Statement(collection.newEntity(), a, Entity("test"), default))
            collection.addStatement(Statement(Entity("a"), Predicate("knows"), Entity("b"), default))
            collection.addStatement(Statement(Entity("b"), Predicate("knows"), Entity("c"), default))
            collection.addStatement(Statement(Entity("c"), Predicate("knows"), Entity("a"), default))
            collection.addStatement(Statement(Entity("c"), Predicate("knows"), Entity("a"), default)) //dupe
            collection.addStatement(Statement(collection.newEntity(), Predicate("fortyTwo"), collection.newEntity(), collection.newEntity()))
            tx.commit()
            val readTx = store.readTx()
            val readCollection: CollectionReadTx = readTx.collection(CollectionName("test"))!!
            readCollection.matchStatements().toSet().size shouldBe 6
            readCollection.matchStatements(null, null, null, default).toSet().size shouldBe 5
            readCollection.matchStatements(null, a, null).toSet() shouldBe setOf(
                    Statement(Entity("This"), a, Entity("test"), default),
                    Statement(Entity("_:1"), a, Entity("test"), default)
            )
            readCollection.matchStatements(null, a, Entity("test")).toSet() shouldBe setOf(
                    Statement(Entity("This"), a, Entity("test"), default),
                    Statement(Entity("_:1"), a, Entity("test"), default)
            )
            readCollection.matchStatements(null, null, Entity("test"), null).toSet() shouldBe setOf(
                    Statement(Entity("This"), a, Entity("test"), default),
                    Statement(Entity("_:1"), a, Entity("test"), default)
            )
            readCollection.matchStatements(null, null, null, Entity("_:4")).toSet() shouldBe setOf(
                    Statement(Entity("_:2"), Predicate("fortyTwo"), Entity("_:3"), Entity("_:4"))
            )
            readTx.cancel() // TODO add test running against a non-existant collection w/ match-statement calls
            store.close()
        }

        "matching statements with literals and ranges in collections" {
            val store = creationFunction()
            val tx = store.writeTx()
            val collection = tx.collection(CollectionName("test"))
            collection shouldNotBe null
            collection.addStatement(Statement(Entity("This"), Predicate("test"), StringLiteral("aa"), default))
            collection.addStatement(Statement(Entity("This"), Predicate("test"), StringLiteral("bb"), default))
            collection.addStatement(Statement(Entity("This"), Predicate("test"), StringLiteral("cc"), default))
            collection.addStatement(Statement(Entity("This"), Predicate("test"), StringLiteral("cd"), default))
            collection.addStatement(Statement(collection.newEntity(), Predicate("test"), LangLiteral("le test", "fr"), default))
            collection.addStatement(Statement(collection.newEntity(), Predicate("test"), LangLiteral("le test", "en"), default))
            collection.addStatement(Statement(collection.newEntity(), Predicate("test"), LangLiteral("le test2", "fr"), default))
            collection.addStatement(Statement(collection.newEntity(), Predicate("test"), LangLiteral("le test3", "fr"), default))
            collection.addStatement(Statement(Entity("a"), Predicate("test"), LongLiteral(100L), default))
            collection.addStatement(Statement(Entity("a"), Predicate("test2"), LongLiteral(100L), default))
            collection.addStatement(Statement(Entity("b"), Predicate("test"), LongLiteral(1000L), default))
            collection.addStatement(Statement(Entity("c"), Predicate("test"), DoubleLiteral(42.0), default))
            collection.addStatement(Statement(Entity("c"), Predicate("test"), DoubleLiteral(42.0), default)) //dupe
            collection.addStatement(Statement(Entity("c"), Predicate("test"), DoubleLiteral(2.0), default))
            collection.addStatement(Statement(collection.newEntity(), a, collection.newEntity(), collection.newEntity()))
            tx.commit()
            val readTx = store.readTx()
            val readCollection = readTx.collection(CollectionName("test"))!!
            readCollection.matchStatements().toSet().size shouldBe 14
            readCollection.matchStatements(null, null, LongLiteral(100L), default).toSet().size shouldBe 2
            readCollection.matchStatements(null, null, StringLiteralRange("b", "cc")).toSet() shouldBe setOf(
                    Statement(Entity("This"), Predicate("test"), StringLiteral("bb"), default),
                    Statement(Entity("This"), Predicate("test"), StringLiteral("cc"), default)
            )
            readCollection.matchStatements(null, null, LangLiteralRange(LangLiteral("le test", "fr"), LangLiteral("le test2", "fr"))).toSet() shouldBe setOf(
                    Statement(Entity("_:1"), Predicate("test"), LangLiteral("le test", "fr"), default),
                    Statement(Entity("_:3"), Predicate("test"), LangLiteral("le test2", "fr"), default)
            )
            readCollection.matchStatements(null, null, LongLiteralRange(99L, 100L), null).toSet() shouldBe setOf(
                    Statement(Entity("a"), Predicate("test"), LongLiteral(100L), default),
                    Statement(Entity("a"), Predicate("test2"), LongLiteral(100L), default)
            )
            readCollection.matchStatements(null, null, DoubleLiteralRange(2.1, 42.0), default).toSet() shouldBe setOf(
                    Statement(Entity("c"), Predicate("test"), DoubleLiteral(42.0), default)
            )
            readTx.cancel() // TODO add test running against a non-existant collection w/ match-statement calls
            store.close()
        }

//    "matching statements with collection literals in collections" {
//        val store = creationFunction()
//        val collection = store.createCollection(Entity("test"))
//        collection shouldNotBe null
//        val tx = collection.writeTx()
//        TODO("Add values")
//        tx.commit()
//        val readTx = collection.readTx()
//        TODO("Add assertions")
//        readTx.cancel() // TODO add test running against a non-existant collection w/ match-statement calls
//    }
    }
}
