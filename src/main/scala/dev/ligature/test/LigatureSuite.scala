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

  test("adding statements to collections") {
    val store = createLigatureSession()
    store.write.use { tx =>
      for {
        ent1 <- tx.newEntity(testCollection)
        ent2 <- tx.newEntity(testCollection)
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
//      val ent1 = tx.newEntity(testCollection)
//      val ent2 = tx.newEntity(testCollection)
//      val ent3 = tx.newEntity(testCollection)
//      tx.addStatement(testCollection, Statement(ent1, a, ent2))
//      tx.addStatement(testCollection, Statement(ent3, a, ent2))
//      tx.removeStatement(testCollection, Statement(ent1, a, ent2))
//    }.runSyncUnsafe()
//    store.read.use { tx =>
//      tx.allStatements(testCollection)
//    }.toSet() shouldBe
//            setOf(Statement(AnonymousNode(3), a, AnonymousNode(2)))
//  }

//        test("new entity test") {
//            val store = createLigatureSession()
//            store.write.use { tx =>
//                tx.addStatement(testCollection, Statement(tx.newEntity(testCollection), a, tx.newEntity(testCollection)))
//                tx.addStatement(testCollection, Statement(tx.newEntity(testCollection), a, tx.newEntity(testCollection)))
//            }
//            store. compute { tx =>
//                tx.allStatements(testCollection)
//            }.toSet() shouldBe setOf(
//                    Statement(AnonymousNode(1), a, AnonymousNode(2)),
//                    Statement(AnonymousNode(4), a, AnonymousNode(5)))
//
//        }
//
//        test("removing named entity") {
//            val store = createLigatureSession()
//            store.write.use { tx =>
//                val ent1 = NamedEntity("a")
//                val ent2 = NamedEntity("b")
//                val ent3 = NamedEntity("c")
//                tx.addStatement(testCollection, Statement(ent1, a, ent2))
//                tx.addStatement(testCollection, Statement(ent3, a, ent2))
//                tx.addStatement(testCollection, Statement(ent2, a, ent1))
//                tx.removeEntity(testCollection, ent1)
//            }
//            store.read.use { tx =>
//                tx.allStatements(testCollection)
//            }.toSet() shouldBe
//                    setOf(Statement(NamedEntity("c"), a, NamedEntity("b")))
//
//        }
//
//        test("removing anonymous entity") {
//            val store = createLigatureSession()
//            store.write.use { tx =>
//                val ent1 = tx.newEntity(testCollection)
//                val ent2 = tx.newEntity(testCollection)
//                val ent3 = tx.newEntity(testCollection)
//                tx.addStatement(testCollection, Statement(ent1, a, ent2))
//                tx.addStatement(testCollection, Statement(ent3, a, ent2))
//                tx.addStatement(testCollection, Statement(ent2, a, ent1))
//                tx.removeEntity(testCollection, ent1)
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
//                val ent1 = tx.newEntity(testCollection)
//                val ent2 = tx.newEntity(testCollection)
//                val ent3 = tx.newEntity(testCollection)
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
//            lateinit var valjean: Entity
//            lateinit var javert: Entity
//            store.write.use { tx =>
//                valjean = tx.newEntity(testCollection)
//                javert = tx.newEntity(testCollection)
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
//            lateinit var valjean: Entity
//            lateinit var javert: Entity
//            lateinit var trout: Entity
//            store.write.use { tx =>
//                valjean = tx.newEntity(testCollection)
//                javert = tx.newEntity(testCollection)
//                trout = tx.newEntity(testCollection)
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
////        val collection = store.createCollection(NamedEntity("test"))
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
