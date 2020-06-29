/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.ligature.test

import dev.ligature._
import dev.ligature.Ligature.a
import monix.eval.Task
import org.scalatest.flatspec.AnyFlatSpec
import monix.execution.Scheduler.Implicits.global
import org.scalatest.matchers.should.Matchers

abstract class LigatureSuite extends AnyFlatSpec with Matchers {
  def createStore(): LigatureStore

  val testCollection: NamedEntity = NamedEntity("test")

  it should "Create and close store" in {
    val store = createStore()

    val c = store.readTx().use( tx => for {
      c <- Task { tx.collections() }
    } yield c)

    c.runSyncUnsafe().toListL.runSyncUnsafe().toSet shouldBe Set()
    store.close()
  }

  it should "creating a new collection" in {
    val store = createStore()

    store.writeTx().use( tx => for {
      x <- Task { tx.createCollection(testCollection) }
    } yield x).runSyncUnsafe()

    val c = store.readTx().use( tx => for {
      c <- Task { tx.collections() }
    } yield c)

    c.runSyncUnsafe().toListL.runSyncUnsafe().toSet shouldBe Set(testCollection)
    store.close()
  }

  it should "access and delete new collection" in {
    val store = createStore()

    store.writeTx().use( tx => for {
      x <- Task { tx.createCollection(testCollection) }
    } yield x).runSyncUnsafe()

    val c = store.readTx().use( tx => for {
      c <- Task { tx.collections() }
    } yield c)

    c.runSyncUnsafe().toListL.runSyncUnsafe().toSet shouldBe Set(testCollection)

    store.writeTx().use( tx => for {
      x <- Task { tx.deleteCollection(testCollection) }
      _ <- Task { tx.deleteCollection(NamedEntity("Test2") )}
    } yield x).runSyncUnsafe()

    val c2 = store.readTx().use( tx => for {
      c <- Task { tx.collections() }
    } yield c)

    c2.runSyncUnsafe().toListL.runSyncUnsafe().toSet shouldBe Set()

    store.close()
  }

  it should "new collections should be empty" in {
    val store = createStore()

    store.writeTx().use( tx => for {
      x <- Task { tx.createCollection(testCollection) }
    } yield x).runSyncUnsafe()

    val s = store.readTx().use( tx => for {
      s <- Task { tx.allStatements(testCollection) }
    } yield s)

    s.runSyncUnsafe().toListL.runSyncUnsafe().toSet shouldBe Set()
    store.close()
  }

  it should "adding statements to collections" in {
    val store = createStore()

    store.writeTx().use( tx => for {
      _ <- Task { tx.addStatement(testCollection, Statement(NamedEntity("Alex"), Ligature.a, NamedEntity("Human"))) }
      r <- Task { tx.addStatement(testCollection, Statement(NamedEntity("Clarice"), Ligature.a, NamedEntity("Feline")) }
    } yield r).runSyncUnsafe()

    val s = store.readTx().use( tx => for {
      s <- Task { tx.allStatements(testCollection) }
    } yield s)

    s.runSyncUnsafe().toListL.runSyncUnsafe().toSet shouldBe
      Set(Statement(NamedEntity("Alex"), Ligature.a, NamedEntity("Human")),
            Statement(NamedEntity("Clarice"), Ligature.a, NamedEntity("Feline")))
    store.close()
  }

  it should "removing statements from collections" in {
    val store = createStore()

    store.writeTx.use( tx => for {
      _ <- Task { tx.addStatement(testCollection, Statement(NamedEntity("Alex"), Ligature.a, NamedEntity("Human"))) }
      _ <- Task { tx.addStatement(testCollection, Statement(NamedEntity("Clarice"), Ligature.a, NamedEntity("Feline"))) }
      x <- Task { tx.removeStatement(testCollection, Statement(NamedEntity("Alex"), Ligature.a, NamedEntity("Human"))) }
    } yield x).runSyncUnsafe()

    val s = store.readTx.use( tx => for {
      s <- Task { tx.allStatements(testCollection) }
    } yield s)
    s.runSyncUnsafe().toListL.runSyncUnsafe().toSet shouldBe
      Set(Statement(NamedEntity("Clarice"), Ligature.a, NamedEntity("Feline")))
    store.close()
  }
  //
  //  "new entity test" {
  //  val store = createStore()
  //  store.writeTx( tx ->
  //  tx.addStatement(testCollection, Statement(tx.newEntity(testCollection), a, tx.newEntity(testCollection)))
  //  tx.addStatement(testCollection, Statement(tx.newEntity(testCollection), a, tx.newEntity(testCollection)))
  //}
  //  store. readTx { tx ->
  //  tx.allStatements(testCollection)
  //}.toSet() shouldBe setOf(
  //  Statement(AnonymousEntity(1), a, AnonymousEntity(2)),
  //  Statement(AnonymousEntity(4), a, AnonymousEntity(5)))
  //  store.close()
  //}
  //
  //  "removing named entity" {
  //  val store = createStore()
  //  store.writeTx( tx ->
  //  val ent1 = NamedEntity("a")
  //  val ent2 = NamedEntity("b")
  //  val ent3 = NamedEntity("c")
  //  tx.addStatement(testCollection, Statement(ent1, a, ent2))
  //  tx.addStatement(testCollection, Statement(ent3, a, ent2))
  //  tx.addStatement(testCollection, Statement(ent2, a, ent1))
  //  tx.removeEntity(testCollection, ent1)
  //}
  //  store.readTx { tx ->
  //  tx.allStatements(testCollection)
  //}.toSet() shouldBe
  //  setOf(Statement(NamedEntity("c"), a, NamedEntity("b")))
  //  store.close()
  //}
  //
  //  "removing anonymous entity" {
  //  val store = createStore()
  //  store.writeTx( tx ->
  //  val ent1 = tx.newEntity(testCollection)
  //  val ent2 = tx.newEntity(testCollection)
  //  val ent3 = tx.newEntity(testCollection)
  //  tx.addStatement(testCollection, Statement(ent1, a, ent2))
  //  tx.addStatement(testCollection, Statement(ent3, a, ent2))
  //  tx.addStatement(testCollection, Statement(ent2, a, ent1))
  //  tx.removeEntity(testCollection, ent1)
  //}
  //  store.readTx { tx ->
  //  tx.allStatements(testCollection)
  //}.toSet() shouldBe
  //  setOf(Statement(AnonymousEntity(3), a, AnonymousEntity(2)))
  //  store.close()
  //}
  //
  //  "removing predicate" {
  //  val store = createStore()
  //  store.writeTx( tx ->
  //  val ent1 = tx.newEntity(testCollection)
  //  val ent2 = tx.newEntity(testCollection)
  //  val ent3 = tx.newEntity(testCollection)
  //  tx.addStatement(testCollection, Statement(ent1, a, ent2))
  //  tx.addStatement(testCollection, Statement(ent3, Predicate("test"), ent2))
  //  tx.addStatement(testCollection, Statement(ent2, a, ent1))
  //  tx.removePredicate(testCollection, a)
  //}
  //  store.readTx { tx ->
  //  tx.allStatements(testCollection)
  //}.toSet() shouldBe
  //  setOf(Statement(AnonymousEntity(3), Predicate("test"), AnonymousEntity(2)))
  //  store.close()
  //}
  //
  //  "matching against a non-existant collection" {
  //  val store = createStore()
  //  store.readTx { tx ->
  //  tx.matchStatements(testCollection, null, null, StringLiteral("French"))
  //  .toSet() shouldBe setOf()
  //  tx.matchStatements(testCollection, null, a, null)
  //  .toSet() shouldBe setOf()
  //}
  //  store.close()
  //}
  //
  //  "matching statements in collections" {
  //  val store = createStore()
  //  lateinit var valjean: Entity
  //  lateinit var javert: Entity
  //  store.writeTx( tx ->
  //  valjean = tx.newEntity(testCollection)
  //  javert = tx.newEntity(testCollection)
  //  tx.addStatement(testCollection, Statement(valjean, Predicate("nationality"), StringLiteral("French")))
  //  tx.addStatement(testCollection, Statement(valjean, Predicate("prisonNumber"), LongLiteral(24601)))
  //  tx.addStatement(testCollection, Statement(javert, Predicate("nationality"), StringLiteral("French")))
  //}
  //  store.readTx { tx ->
  //  tx.matchStatements(testCollection, null, null, StringLiteral("French"))
  //  .toSet() shouldBe setOf(
  //  Statement(valjean, Predicate("nationality"), StringLiteral("French")),
  //  Statement(javert, Predicate("nationality"), StringLiteral("French"))
  //  )
  //  tx.matchStatements(testCollection, null, null, LongLiteral(24601))
  //  .toSet() shouldBe setOf(
  //  Statement(valjean, Predicate("prisonNumber"), LongLiteral(24601))
  //  )
  //  tx.matchStatements(testCollection, valjean)
  //  .toSet() shouldBe setOf(
  //  Statement(valjean, Predicate("nationality"), StringLiteral("French")),
  //  Statement(valjean, Predicate("prisonNumber"), LongLiteral(24601))
  //  )
  //  tx.matchStatements(testCollection, javert, Predicate("nationality"), StringLiteral("French"))
  //  .toSet() shouldBe setOf(
  //  Statement(javert, Predicate("nationality"), StringLiteral("French"))
  //  )
  //  tx.matchStatements(testCollection, null, null, null)
  //  .toSet() shouldBe setOf(
  //  Statement(valjean, Predicate("nationality"), StringLiteral("French")),
  //  Statement(valjean, Predicate("prisonNumber"), LongLiteral(24601)),
  //  Statement(javert, Predicate("nationality"), StringLiteral("French"))
  //  )
  //}
  //  store.close()
  //}
  //
  //  "matching statements with literals and ranges in collections" {
  //  val store = createStore()
  //  lateinit var valjean: Entity
  //  lateinit var javert: Entity
  //  lateinit var trout: Entity
  //  store.writeTx( tx ->
  //  valjean = tx.newEntity(testCollection)
  //  javert = tx.newEntity(testCollection)
  //  trout = tx.newEntity(testCollection)
  //  tx.addStatement(testCollection, Statement(valjean, Predicate("nationality"), StringLiteral("French")))
  //  tx.addStatement(testCollection, Statement(valjean, Predicate("prisonNumber"), LongLiteral(24601)))
  //  tx.addStatement(testCollection, Statement(javert, Predicate("nationality"), StringLiteral("French")))
  //  tx.addStatement(testCollection, Statement(javert, Predicate("prisonNumber"), LongLiteral(24602)))
  //  tx.addStatement(testCollection, Statement(trout, Predicate("nationality"), StringLiteral("American")))
  //  tx.addStatement(testCollection, Statement(trout, Predicate("prisonNumber"), LongLiteral(24603)))
  //}
  //  store.readTx { tx ->
  //  tx.matchStatements(testCollection, null, null, StringLiteralRange("French", "German"))
  //  .toSet() shouldBe setOf(
  //  Statement(valjean, Predicate("nationality"), StringLiteral("French")),
  //  Statement(javert, Predicate("nationality"), StringLiteral("French"))
  //  )
  //  tx.matchStatements(testCollection, null, null, LongLiteralRange(24601, 24603))
  //  .toSet() shouldBe setOf(
  //  Statement(valjean, Predicate("prisonNumber"), LongLiteral(24601)),
  //  Statement(javert, Predicate("prisonNumber"), LongLiteral(24602))
  //  )
  //  tx.matchStatements(testCollection, valjean, null, LongLiteralRange(24601, 24603))
  //  .toSet() shouldBe setOf(
  //  Statement(valjean, Predicate("prisonNumber"), LongLiteral(24601))
  //  )
  //}
  //  store.close()
  //}
  //
  //  //    "matching statements with collection literals in collections" {
  //  //        val store = createStore()
  //  //        val collection = store.createCollection(NamedEntity("test"))
  //  //        collection shouldNotBe null
  //  //        val tx = collection.writeTx()
  //  //        TODO("Add values")
  //  //        tx.commit()
  //  //        val tx = collection.tx()
  //  //        TODO("Add assertions")
  //  //        tx.cancel() // TODO add test running against a non-existant collection w/ match-statement calls
  //  //    }
  //}
  //}
}
