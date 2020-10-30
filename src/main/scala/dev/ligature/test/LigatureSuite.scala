/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.ligature.test

import dev.ligature._
import dev.ligature.Ligature.a
import munit._

abstract class LigatureSuite extends FunSuite {
  def createLigature: Ligature

  val testCollection: NamedNode = NamedNode("test")

  test("Create and close store") {
    val store = createLigature.instance.use { instance  =>
    val res = instance.read.use { tx =>
      tx.collections.toListL
    }.runSyncUnsafe().toSet
    assert(res.isEmpty)
  }

//  test("creating a new collection") {
//    val store = createLigature.instance.use { instance  =>
//    instance.write.use { tx =>
//      tx.createCollection(testCollection)
//    }.runSyncUnsafe()
//    val res = instance.read.use { tx =>
//      tx.collections.toListL
//    }.runSyncUnsafe().toSet
//    assertEquals(res, Set(testCollection))
//  }
//
//  test("access and delete new collection") {
//    val store = createLigature.instance.use { instance  =>
//    instance.write.use { tx =>
//      tx.createCollection(testCollection)
//    }.runSyncUnsafe()
//    instance.write.use { tx =>
//      for {
//        _ <- tx.deleteCollection(testCollection)
//        _ <- tx.deleteCollection(NamedNode("test2"))
//      } yield()
//    }.runSyncUnsafe()
//    val res2 = instance.read.use { tx =>
//      tx.collections.toListL
//    }.runSyncUnsafe()
//    assert(res2.isEmpty)
//  }
//
//  test("new collections should be empty") {
//    val store = createLigature.instance.use { instance  =>
//    instance.write.use { tx =>
//      tx.createCollection(testCollection)
//    }.runSyncUnsafe()
//    val res = instance.read.use { tx =>
//      tx.allStatements(testCollection).toListL
//    }.runSyncUnsafe().toSet
//    assert(res.isEmpty)
//  }
//
//  test("new node test") {
//    val store = createLigature.instance.use { instance  =>
//    instance.write.use { tx =>
//      for {
//        nn1 <- tx.newNode(testCollection)
//        nn2 <- tx.newNode(testCollection)
//        _   <- tx.addStatement(testCollection, Statement(nn1, a, nn2))
//        nn3 <- tx.newNode(testCollection)
//        nn4 <- tx.newNode(testCollection)
//        _   <- tx.addStatement(testCollection, Statement(nn3, a, nn4))
//      } yield ()
//    }.runSyncUnsafe()
//    val res = instance.read.use { tx =>
//      tx.allStatements(testCollection).toListL
//    }.runSyncUnsafe().toSet
//    assertEquals(res.map { _.statement }, Set(
//      Statement(AnonymousNode(1), a, AnonymousNode(2)),
//      Statement(AnonymousNode(4), a, AnonymousNode(5))))
//  }
//
//  test("adding statements to collections") {
//    val store = createLigature.instance.use { instance  =>
//    instance.write.use { tx =>
//      for {
//        ent1 <- tx.newNode(testCollection)
//        ent2 <- tx.newNode(testCollection)
//        _    <- tx.addStatement(testCollection, Statement(ent1, a, ent2))
//        _    <- tx.addStatement(testCollection, Statement(ent1, a, ent2))
//      } yield()
//    }.runSyncUnsafe()
//    val res  = instance.read.use { tx =>
//      tx.allStatements(testCollection).map { _.statement }.toListL
//    }.runSyncUnsafe().toSet
//    assertEquals(res, Set(Statement(AnonymousNode(1), a, AnonymousNode(2)),
//      Statement(AnonymousNode(1), a, AnonymousNode(2))))
//  }
//
//  test("removing statements from collections") {
//    val store = createLigature.instance.use { instance  =>
//    instance.write.use { tx =>
//      for {
//        nn1 <- tx.newNode(testCollection)
//        nn2 <- tx.newNode(testCollection)
//        nn3 <- tx.newNode(testCollection)
//        _   <- tx.addStatement(testCollection, Statement(nn1, a, nn2))
//        _   <- tx.addStatement(testCollection, Statement(nn3, a, nn2))
//        _   <- tx.removeStatement(testCollection, Statement(nn1, a, nn2))
//        _   <- tx.removeStatement(testCollection, Statement(nn1, a, nn2))
//        _   <- tx.removeStatement(testCollection, Statement(nn2, a, nn1))
//      } yield()
//    }.runSyncUnsafe()
//    val res = instance.read.use { tx =>
//      tx.allStatements(testCollection).map { _.statement }.toListL
//    }.runSyncUnsafe().toSet
//    assertEquals(res, Set(Statement(AnonymousNode(3), a, AnonymousNode(2))))
//  }
//fc//yvy            tyc  yy    val store = createLigature.instance.use { instance  =>
////    val (r1, r2) = instance.read.use { tx =>
////      for {
////        r1 <- tx.matchStatements(testCollection, null, null, StringLiteral("French")).toListL
////        r2 <- tx.matchStatements(testCollection, null, a, null).toListL
////      } yield(r1, r2)
////    }
////  }
////
////  test("matching statements in collections") {
////    val store = createLigature.instance.use { instance  =>
////    lateinit var valjean: Node
////    lateinit var javert: Node
////    instance.write.use { tx =>
////      valjean = tx.newNode(testCollection)
////      javert = tx.newNode(testCollection)
////      tx.addStatement(testCollection, Statement(valjean, Predicate("nationality"), StringLiteral("French")))
////      tx.addStatement(testCollection, Statement(valjean, Predicate("prisonNumber"), LongLiteral(24601)))
////      tx.addStatement(testCollection, Statement(javert, Predicate("nationality"), StringLiteral("French")))
////    }
////    instance.read.use { tx =>
////      tx.matchStatements(testCollection, null, null, StringLiteral("French"))
////              .toSet() shouldBe setOf(
////                  Statement(valjean, Predicate("nationality"), StringLiteral("French")),
////                  Statement(javert, Predicate("nationality"), StringLiteral("French"))
////      )
////      tx.matchStatements(testCollection, null, null, LongLiteral(24601))
////              .toSet() shouldBe setOf(
////                  Statement(valjean, Predicate("prisonNumber"), LongLiteral(24601))
////      )
////      tx.matchStatements(testCollection, valjean)
////              .toSet() shouldBe setOf(
////                  Statement(valjean, Predicate("nationality"), StringLiteral("French")),
////                  Statement(valjean, Predicate("prisonNumber"), LongLiteral(24601))
////      )
////      tx.matchStatements(testCollection, javert, Predicate("nationality"), StringLiteral("French"))
////              .toSet() shouldBe setOf(
////                  Statement(javert, Predicate("nationality"), StringLiteral("French"))
////      )
////      tx.matchStatements(testCollection, null, null, null)
////              .toSet() shouldBe setOf(
////                  Statement(valjean, Predicate("nationality"), StringLiteral("French")),
////                  Statement(valjean, Predicate("prisonNumber"), LongLiteral(24601)),
////                  Statement(javert, Predicate("nationality"), StringLiteral("French"))
////      )
////    }
////  }
////
////  test("matching statements with literals and ranges in collections") {
////    val store = createLigature.instance.use { instance  =>
////    lateinit var valjean: Node
////    lateinit var javert: Node
////    lateinit var trout: Node
////    instance.write.use { tx =>
////      valjean = tx.newNode(testCollection)
////      javert = tx.newNode(testCollection)
////      trout = tx.newNode(testCollection)
////      tx.addStatement(testCollection, Statement(valjean, Predicate("nationality"), StringLiteral("French")))
////      tx.addStatement(testCollection, Statement(valjean, Predicate("prisonNumber"), LongLiteral(24601)))
////      tx.addStatement(testCollection, Statement(javert, Predicate("nationality"), StringLiteral("French")))
////      tx.addStatement(testCollection, Statement(javert, Predicate("prisonNumber"), LongLiteral(24602)))
////      tx.addStatement(testCollection, Statement(trout, Predicate("nationality"), StringLiteral("American")))
////      tx.addStatement(testCollection, Statement(trout, Predicate("prisonNumber"), LongLiteral(24603)))
////    }
////    instance.read.use { tx =>
////      tx.matchStatements(testCollection, null, null, StringLiteralRange("French", "German"))
////              .toSet() shouldBe setOf(
////                  Statement(valjean, Predicate("nationality"), StringLiteral("French")),
////                  Statement(javert, Predicate("nationality"), StringLiteral("French"))
////      )
////      tx.matchStatements(testCollection, null, null, LongLiteralRange(24601, 24603))
////              .toSet() shouldBe setOf(
////                  Statement(valjean, Predicate("prisonNumber"), LongLiteral(24601)),
////                  Statement(javert, Predicate("prisonNumber"), LongLiteral(24602))
////      )
////      tx.matchStatements(testCollection, valjean, null, LongLiteralRange(24601, 24603))
////              .toSet() shouldBe setOf(
////                  Statement(valjean, Predicate("prisonNumber"), LongLiteral(24601))
////      )
////    }
////  }
////
////  test("matching statements with collection literals in collections") {
////    val store = createLigature.instance.use { instance  =>
////    val collection = store.createCollection(NamedNode("test"))
////    collection shouldNotBe null
////    val tx = collection.writeTx()
////    TODO("Add values")
////    tx.commit()
////    val tx = collection.tx()
////    TODO("Add assertions")
////    tx.cancel() // TODO add test running against a non-existant collection w/ match-statement calls
////  }
}
