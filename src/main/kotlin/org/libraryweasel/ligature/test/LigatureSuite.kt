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
            store.details() shouldBe mapOf("location" to "memory")
            store.allCollections().toList() shouldBe listOf<Entity>()
            store.close()
        }

        "access new collection" {
            val store = creationFunction()
            store.collection(Entity("test")) shouldNotBe null
            store.allCollections().toList() shouldBe listOf<Entity>()
        }

        "creating a new collection" {
            val store = creationFunction()
            store.createCollection(Entity("test")) shouldNotBe null
            store.allCollections().toList() shouldBe listOf<Entity>(Entity("test"))
        }

        "access and delete new collection" {
            val store = creationFunction()
            store.createCollection(Entity("test")) shouldNotBe null
            store.allCollections().toList() shouldBe listOf<Entity>(Entity("test"))
            store.deleteCollection(Entity("test"))
            store.deleteCollection(Entity("test2"))
            store.allCollections().toList() shouldBe listOf<Entity>()
        }

        "new collections should be empty" {
            val store = creationFunction()
            val collection = store.createCollection(Entity("test"))
            collection shouldNotBe null
            val tx = collection.readTx()
            tx.allStatements().toList() shouldBe listOf()
            tx.allRules().toList() shouldBe listOf()
            tx.cancel()
        }

        "adding statements to collections" {
            val store = creationFunction()
            val collection = store.createCollection(Entity("test"))
            collection shouldNotBe null
            val tx = collection.writeTx()
            tx.addStatement(Statement(Entity("This"), a, Entity("test"), default))
            tx.commit()
            val readTx = collection.readTx()
            readTx.allStatements().toList() shouldBe listOf(Statement(Entity("This"), a, Entity("test"), default))
            readTx.cancel()
        }

        "adding rules to collections" {
            val store = creationFunction()
            val collection = store.createCollection(Entity("test"))
            collection shouldNotBe null
            val tx = collection.writeTx()
            tx.addRule(Rule(Entity("Also"), a, Entity("test")))
            tx.commit()
            val readTx = collection.readTx()
            readTx.allRules().toList() shouldBe listOf(Rule(Entity("Also"), a, Entity("test")))
            readTx.cancel()
        }

        "removing statements from collections" {
            val store = creationFunction()
            val collection = store.createCollection(Entity("test"))
            collection shouldNotBe null
            val tx = collection.writeTx()
            tx.addStatement(Statement(Entity("This"), a, Entity("test"), default))
            tx.addStatement(Statement(Entity("Also"), a, Entity("test"), default))
            tx.removeStatement(Statement(Entity("This"), a, Entity("test"), default))
            tx.commit()
            val readTx = collection.readTx()
            readTx.allStatements().toList() shouldBe listOf(Statement(Entity("Also"), a, Entity("test"), default))
            readTx.cancel()
        }

        "removing rules from collections" {
            val store = creationFunction()
            val collection = store.createCollection(Entity("test"))
            collection shouldNotBe null
            val tx = collection.writeTx()
            tx.addRule(Rule(Entity("This"), a, Entity("test")))
            tx.addRule(Rule(Entity("Also"), a, Entity("test")))
            tx.removeRule(Rule(Entity("This"), a, Entity("test")))
            tx.commit()
            val readTx = collection.readTx()
            readTx.allRules().toList() shouldBe listOf(Rule(Entity("Also"), a, Entity("test")))
            readTx.cancel()
        }

        "new entity test" {
            val store = creationFunction()
            val collection = store.createCollection(Entity("test"))
            collection shouldNotBe null
            val tx = collection.writeTx()
            tx.addStatement(Statement(tx.newEntity(), tx.newEntity(), tx.newEntity(), tx.newEntity()))
            tx.addStatement(Statement(tx.newEntity(), tx.newEntity(), tx.newEntity(), tx.newEntity()))
            tx.commit()
            val readTx = collection.readTx()
            readTx.allStatements().toSet() shouldBe setOf(
                    Statement(Entity("_:1"), Entity("_:2"), Entity("_:3"), Entity("_:4")),
                    Statement(Entity("_:5"), Entity("_:6"), Entity("_:7"), Entity("_:8")))
            readTx.cancel()
        }

        "matching rules in collections" {
            val store = creationFunction()
            val collection = store.createCollection(Entity("test"))
            collection shouldNotBe null
            val tx = collection.writeTx()
            tx.addRule(Rule(Entity("This"), a, Entity("test")))
            tx.addRule(Rule(tx.newEntity(), a, Entity("test")))
            tx.addRule(Rule(Entity("a"), Entity("knows"), Entity("b")))
            tx.addRule(Rule(Entity("b"), Entity("knows"), Entity("c")))
            tx.addRule(Rule(Entity("c"), Entity("knows"), Entity("a")))
            tx.addRule(Rule(Entity("c"), Entity("knows"), Entity("a"))) //dupe
            tx.addRule(Rule(tx.newEntity(), tx.newEntity(), tx.newEntity()))
            tx.commit()
            val readTx = collection.readTx()
            readTx.matchRules().toSet().size shouldBe 6
            readTx.matchRules(null, null, null).toSet().size shouldBe 6
            readTx.matchRules(null, a, null).toSet() shouldBe setOf(
                    Rule(Entity("This"), a, Entity("test")),
                    Rule(Entity("_:1"), a, Entity("test"))
            )
            readTx.matchRules(null, a, Entity("test")).toSet() shouldBe setOf(
                    Rule(Entity("This"), a, Entity("test")),
                    Rule(Entity("_:1"), a, Entity("test"))
            )
            readTx.matchRules(null, null, Entity("test")).toSet() shouldBe setOf(
                    Rule(Entity("This"), a, Entity("test")),
                    Rule(Entity("_:1"), a, Entity("test"))
            )
            readTx.cancel() // TODO add test running against a non-existant collection w/ match-statement calls
        }

        "matching statements in collections" {
            val store = creationFunction()
            val collection = store.createCollection(Entity("test"))
            collection shouldNotBe null
            val tx = collection.writeTx()
            tx.addStatement(Statement(Entity("This"), a, Entity("test"), default))
            tx.addStatement(Statement(tx.newEntity(), a, Entity("test"), default))
            tx.addStatement(Statement(Entity("a"), Entity("knows"), Entity("b"), default))
            tx.addStatement(Statement(Entity("b"), Entity("knows"), Entity("c"), default))
            tx.addStatement(Statement(Entity("c"), Entity("knows"), Entity("a"), default))
            tx.addStatement(Statement(Entity("c"), Entity("knows"), Entity("a"), default)) //dupe
            tx.addStatement(Statement(tx.newEntity(), tx.newEntity(), tx.newEntity(), tx.newEntity()))
            tx.commit()
            val readTx = collection.readTx()
            readTx.matchStatements().toSet().size shouldBe 6
            readTx.matchStatements(null, null, null, default).toSet().size shouldBe 5
            readTx.matchStatements(null, a, null).toSet() shouldBe setOf(
                    Statement(Entity("This"), a, Entity("test"), default),
                    Statement(Entity("_:1"), a, Entity("test"), default)
            )
            readTx.matchStatements(null, a, Entity("test")).toSet() shouldBe setOf(
                    Statement(Entity("This"), a, Entity("test"), default),
                    Statement(Entity("_:1"), a, Entity("test"), default)
            )
            readTx.matchStatements(null, null, Entity("test"), null).toSet() shouldBe setOf(
                    Statement(Entity("This"), a, Entity("test"), default),
                    Statement(Entity("_:1"), a, Entity("test"), default)
            )
            readTx.matchStatements(null, null, null, Entity("_:5")).toSet() shouldBe setOf(
                    Statement(Entity("_:2"), Entity("_:3"), Entity("_:4"), Entity("_:5"))
            )
            readTx.cancel() // TODO add test running against a non-existant collection w/ match-statement calls
        }

        "matching statements with literals and ranges in collections" {
            val store = creationFunction()
            val collection = store.createCollection(Entity("test"))
            collection shouldNotBe null
            val tx = collection.writeTx()
            tx.addStatement(Statement(Entity("This"), Entity("test"), StringLiteral("aa"), default))
            tx.addStatement(Statement(Entity("This"), Entity("test"), StringLiteral("bb"), default))
            tx.addStatement(Statement(Entity("This"), Entity("test"), StringLiteral("cc"), default))
            tx.addStatement(Statement(Entity("This"), Entity("test"), StringLiteral("cd"), default))
            tx.addStatement(Statement(tx.newEntity(), Entity("test"), LangLiteral("le test", "fr"), default))
            tx.addStatement(Statement(tx.newEntity(), Entity("test"), LangLiteral("le test", "en"), default))
            tx.addStatement(Statement(tx.newEntity(), Entity("test"), LangLiteral("le test2", "fr"), default))
            tx.addStatement(Statement(tx.newEntity(), Entity("test"), LangLiteral("le test3", "fr"), default))
            tx.addStatement(Statement(Entity("a"), Entity("test"), LongLiteral(100L), default))
            tx.addStatement(Statement(Entity("a"), Entity("test2"), LongLiteral(100L), default))
            tx.addStatement(Statement(Entity("b"), Entity("test"), LongLiteral(1000L), default))
            tx.addStatement(Statement(Entity("c"), Entity("test"), DoubleLiteral(42.0), default))
            tx.addStatement(Statement(Entity("c"), Entity("test"), DoubleLiteral(42.0), default)) //dupe
            tx.addStatement(Statement(Entity("c"), Entity("test"), DoubleLiteral(2.0), default))
            tx.addStatement(Statement(tx.newEntity(), tx.newEntity(), tx.newEntity(), tx.newEntity()))
            tx.commit()
            val readTx = collection.readTx()
            readTx.matchStatements().toSet().size shouldBe 14
            readTx.matchStatements(null, null, LongLiteral(100L), default).toSet().size shouldBe 2
            readTx.matchStatements(null, null, StringLiteralRange("b", "cc")).toSet() shouldBe setOf(
                    Statement(Entity("This"), Entity("test"), StringLiteral("bb"), default),
                    Statement(Entity("This"), Entity("test"), StringLiteral("cc"), default)
            )
            readTx.matchStatements(null, null, LangLiteralRange(LangLiteral("le test", "fr"), LangLiteral("le test2", "fr"))).toSet() shouldBe setOf(
                    Statement(Entity("_:1"), Entity("test"), LangLiteral("le test", "fr"), default),
                    Statement(Entity("_:3"), Entity("test"), LangLiteral("le test2", "fr"), default)
            )
            readTx.matchStatements(null, null, LongLiteralRange(99L, 100L), null).toSet() shouldBe setOf(
                    Statement(Entity("a"), Entity("test"), LongLiteral(100L), default),
                    Statement(Entity("a"), Entity("test2"), LongLiteral(100L), default)
            )
            readTx.matchStatements(null, null, DoubleLiteralRange(2.1, 42.0), default).toSet() shouldBe setOf(
                    Statement(Entity("c"), Entity("test"), DoubleLiteral(42.0), default)
            )
            readTx.cancel() // TODO add test running against a non-existant collection w/ match-statement calls
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
