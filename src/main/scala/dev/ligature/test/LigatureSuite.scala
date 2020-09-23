/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.ligature.test

import dev.ligature._
import dev.ligature.Ligature.a
import munit._
import monix.execution.Scheduler.Implicits.global

abstract class LigatureSuite extends FunSuite {
  def createLigatureSession(): LigatureSession

  val testCollection: NamedNode = NamedNode("test")

  test("Create and close store") {
    val store = createLigatureSession()
    val res = store.read.use { tx =>
      tx.collections().toListL
    }.runSyncUnsafe().toSet
    assert(res.isEmpty)
  }

  test("creating a new collection") {
    val store = createLigatureSession()
    store.write.use { tx =>
      tx.createCollection(testCollection)
    }.runSyncUnsafe()
    val res = store.read.use { tx =>
      tx.collections().toListL
    }.runSyncUnsafe().toSet
    assert(res == Set(testCollection))
  }

  test("access and delete new collection") {
    val store = createLigatureSession()
    store.write.use { tx =>
      tx.createCollection(testCollection)
    }.runSyncUnsafe()
    val res = store.read.use { tx =>
      tx.collections().toListL
    }.runSyncUnsafe().toSet
    store.write.use { tx =>
      for {
        _ <- tx.deleteCollection(testCollection)
        _ <- tx.deleteCollection(NamedNode("test2"))
      } yield()
    }.runSyncUnsafe()
    val res2 = store.read.use { tx =>
      tx.collections().toListL
    }.runSyncUnsafe()
    assert(res2.isEmpty)
  }

  test("new collections should be empty") {
    val store = createLigatureSession()
    store.write.use { tx =>
      tx.createCollection(testCollection)
    }.runSyncUnsafe()
    val res = store.read.use { tx =>
      tx.allStatements(testCollection).toListL
    }.runSyncUnsafe().toSet
    assert(res.isEmpty)
  }

  test("new node test") {
    val store = createLigatureSession()
    store.write.use { tx =>
      for {
        nn1 <- tx.newNode(testCollection)
        nn2 <- tx.newNode(testCollection)
        _   <- tx.addStatement(testCollection, Statement(nn1, a, nn2))
        nn3 <- tx.newNode(testCollection)
        nn4 <- tx.newNode(testCollection)
        _   <- tx.addStatement(testCollection, Statement(nn3, a, nn4))
      } yield ()
    }.runSyncUnsafe()
    val res = store.read.use { tx =>
      tx.allStatements(testCollection).toListL
    }.runSyncUnsafe().toSet
    assert(res.map { _.statement } == Set(
      Statement(AnonymousNode(1), a, AnonymousNode(2)),
      Statement(AnonymousNode(4), a, AnonymousNode(5))))
  }

  test("adding statements to collections") {
    val store = createLigatureSession()
    store.write.use { tx =>
      for {
        ent1 <- tx.newNode(testCollection)
        ent2 <- tx.newNode(testCollection)
        _    <- tx.addStatement(testCollection, Statement(ent1, a, ent2))
        _    <- tx.addStatement(testCollection, Statement(ent1, a, ent2))
      } yield()
    }.runSyncUnsafe()
    val res  = store.read.use { tx =>
      tx.allStatements(testCollection).map { _.statement }.toListL
    }.runSyncUnsafe().toSet
    assert(res == Set(Statement(AnonymousNode(1), a, AnonymousNode(2)),
      Statement(AnonymousNode(1), a, AnonymousNode(2))))
  }

//  test("removing statements from collections") {
//    val store = createLigatureSession()
//    store.write.use { tx =>
//      val ent1 = tx.newNode(testCollection)
//      val ent2 = tx.newNode(testCollection)
//      val ent3 = tx.newNode(testCollection)
//      tx.addStatement(testCollection, Statement(ent1, a, ent2))
//      tx.addStatement(testCollection, Statement(ent3, a, ent2))
//      tx.removeStatement(testCollection, Statement(ent1, a, ent2))
//    }.runSyncUnsafe()
//    store.read.use { tx =>
//      tx.allStatements(testCollection)
//    }.toSet() shouldBe
//            setOf(Statement(AnonymousNode(3), a, AnonymousNode(2)))
//  }
//
//        test("removing named node") {
//            val store = createLigatureSession()
//            store.write.use { tx =>
//                val ent1 = NamedNode("a")
//                val ent2 = NamedNode("b")
//                val ent3 = NamedNode("c")
//                tx.addStatement(testCollection, Statement(ent1, a, ent2))
//                tx.addStatement(testCollection, Statement(ent3, a, ent2))
//                tx.addStatement(testCollection, Statement(ent2, a, ent1))
//                tx.removeNode(testCollection, ent1)
//            }
//            store.read.use { tx =>
//                tx.allStatements(testCollection)
//            }.toSet() shouldBe
//                    setOf(Statement(NamedNode("c"), a, NamedNode("b")))
//
//        }
//
//        test("removing anonymous node") {
//            val store = createLigatureSession()
//            store.write.use { tx =>
//                val ent1 = tx.newNode(testCollection)
//                val ent2 = tx.newNode(testCollection)
//                val ent3 = tx.newNode(testCollection)
//                tx.addStatement(testCollection, Statement(ent1, a, ent2))
//                tx.addStatement(testCollection, Statement(ent3, a, ent2))
//                tx.addStatement(testCollection, Statement(ent2, a, ent1))
//                tx.removeNode(testCollection, ent1)
//            }
//            store.read.use { tx =>
//                tx.allStatements(testCollection)
//            }.toSet() shouldBe
//                    setOf(Statement(AnonymousNode(3), a, AnonymousNode(2)))
//
//        }
//
//        test("removing predicate") {
//            val store = createLigatureSession()
//            store.write.use { tx =>
//                val ent1 = tx.newNode(testCollection)
//                val ent2 = tx.newNode(testCollection)
//                val ent3 = tx.newNode(testCollection)
//                tx.addStatement(testCollection, Statement(ent1, a, ent2))
//                tx.addStatement(testCollection, Statement(ent3, Predicate("test"), ent2))
//                tx.addStatement(testCollection, Statement(ent2, a, ent1))
//                tx.removePredicate(testCollection, a)
//            }
//            store.read.use { tx =>
//                tx.allStatements(testCollection)
//            }.toSet() shouldBe
//                    setOf(Statement(AnonymousNode(3), Predicate("test"), AnonymousNode(2)))
//
//        }
//
//        test("matching against a non-existant collection") {
//            val store = createLigatureSession()
//            store.read.use { tx =>
//                tx.matchStatements(testCollection, null, null, StringLiteral("French"))
//                    .toSet() shouldBe setOf()
//                tx.matchStatements(testCollection, null, a, null)
//                    .toSet() shouldBe setOf()
//            }
//
//        }
//
//        test("matching statements in collections") {
//            val store = createLigatureSession()
//            lateinit var valjean: Node
//            lateinit var javert: Node
//            store.write.use { tx =>
//                valjean = tx.newNode(testCollection)
//                javert = tx.newNode(testCollection)
//                tx.addStatement(testCollection, Statement(valjean, Predicate("nationality"), StringLiteral("French")))
//                tx.addStatement(testCollection, Statement(valjean, Predicate("prisonNumber"), LongLiteral(24601)))
//                tx.addStatement(testCollection, Statement(javert, Predicate("nationality"), StringLiteral("French")))
//            }
//            store.read.use { tx =>
//                tx.matchStatements(testCollection, null, null, StringLiteral("French"))
//                        .toSet() shouldBe setOf(
//                            Statement(valjean, Predicate("nationality"), StringLiteral("French")),
//                            Statement(javert, Predicate("nationality"), StringLiteral("French"))
//                )
//                tx.matchStatements(testCollection, null, null, LongLiteral(24601))
//                        .toSet() shouldBe setOf(
//                            Statement(valjean, Predicate("prisonNumber"), LongLiteral(24601))
//                )
//                tx.matchStatements(testCollection, valjean)
//                        .toSet() shouldBe setOf(
//                            Statement(valjean, Predicate("nationality"), StringLiteral("French")),
//                            Statement(valjean, Predicate("prisonNumber"), LongLiteral(24601))
//                )
//                tx.matchStatements(testCollection, javert, Predicate("nationality"), StringLiteral("French"))
//                        .toSet() shouldBe setOf(
//                            Statement(javert, Predicate("nationality"), StringLiteral("French"))
//                )
//                tx.matchStatements(testCollection, null, null, null)
//                        .toSet() shouldBe setOf(
//                            Statement(valjean, Predicate("nationality"), StringLiteral("French")),
//                            Statement(valjean, Predicate("prisonNumber"), LongLiteral(24601)),
//                            Statement(javert, Predicate("nationality"), StringLiteral("French"))
//                )
//            }
//
//        }
//
//        test("matching statements with literals and ranges in collections") {
//            val store = createLigatureSession()
//            lateinit var valjean: Node
//            lateinit var javert: Node
//            lateinit var trout: Node
//            store.write.use { tx =>
//                valjean = tx.newNode(testCollection)
//                javert = tx.newNode(testCollection)
//                trout = tx.newNode(testCollection)
//                tx.addStatement(testCollection, Statement(valjean, Predicate("nationality"), StringLiteral("French")))
//                tx.addStatement(testCollection, Statement(valjean, Predicate("prisonNumber"), LongLiteral(24601)))
//                tx.addStatement(testCollection, Statement(javert, Predicate("nationality"), StringLiteral("French")))
//                tx.addStatement(testCollection, Statement(javert, Predicate("prisonNumber"), LongLiteral(24602)))
//                tx.addStatement(testCollection, Statement(trout, Predicate("nationality"), StringLiteral("American")))
//                tx.addStatement(testCollection, Statement(trout, Predicate("prisonNumber"), LongLiteral(24603)))
//            }
//            store.read.use { tx =>
//                tx.matchStatements(testCollection, null, null, StringLiteralRange("French", "German"))
//                        .toSet() shouldBe setOf(
//                            Statement(valjean, Predicate("nationality"), StringLiteral("French")),
//                            Statement(javert, Predicate("nationality"), StringLiteral("French"))
//                )
//                tx.matchStatements(testCollection, null, null, LongLiteralRange(24601, 24603))
//                        .toSet() shouldBe setOf(
//                            Statement(valjean, Predicate("prisonNumber"), LongLiteral(24601)),
//                            Statement(javert, Predicate("prisonNumber"), LongLiteral(24602))
//                )
//                tx.matchStatements(testCollection, valjean, null, LongLiteralRange(24601, 24603))
//                        .toSet() shouldBe setOf(
//                            Statement(valjean, Predicate("prisonNumber"), LongLiteral(24601))
//                )
//            }
//
//        }
//
////    test("matching statements with collection literals in collections") {
////        val store = createLigatureSession()
////        val collection = store.createCollection(NamedNode("test"))
////        collection shouldNotBe null
////        val tx = collection.writeTx()
////        TODO("Add values")
////        tx.commit()
////        val tx = collection.tx()
////        TODO("Add assertions")
////        tx.cancel() // TODO add test running against a non-existant collection w/ match-statement calls
////    }
//    }
//}
}
