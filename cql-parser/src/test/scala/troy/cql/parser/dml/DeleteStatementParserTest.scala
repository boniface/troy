package troy.cql.parser.dml

import org.scalatest.{ FlatSpec, Matchers }
import troy.cql.ast.dml.SimpleSelection._
import troy.cql.ast.dml.{ IfExist, IfCondition }
import troy.cql.ast.{ Constant, TupleLiteral }
import troy.cql.ast.dml._
import troy.cql.ast.dml.WhereClause.Relation.Simple
import troy.cql.ast.DeleteStatement
import troy.cql.parser.ParserTestUtils.parseQuery

class DeleteStatementParserTest extends FlatSpec with Matchers {
  "Delete Parser" should "parse simple delete statement" in {
    val statement = parseQuery("DELETE FROM NerdMovies WHERE movie = 'Serenity';").asInstanceOf[DeleteStatement]
    statement.simpleSelection.isEmpty shouldBe true
    statement.from.table shouldBe "NerdMovies"
    statement.using.isEmpty shouldBe true

    val relations = statement.where.relations
    relations.size shouldBe 1
    val simpleRelation = relations(0).asInstanceOf[Simple]
    simpleRelation.columnName shouldBe "movie"
    simpleRelation.operator shouldBe Operator.Equals
    simpleRelation.term.asInstanceOf[Constant].raw shouldBe "Serenity"

  }

  it should "parse simple delete statement with UUID value" in {
    val statement = parseQuery("DELETE FROM Users WHERE userid = 01234567-0123-0123-0123-0123456789ab;")
      .asInstanceOf[DeleteStatement]
    statement.simpleSelection.isEmpty shouldBe true
    statement.from.table shouldBe "Users"
    statement.using.isEmpty shouldBe true

    val relations = statement.where.relations
    relations.size shouldBe 1
    val simpleRelation = relations(0).asInstanceOf[Simple]
    simpleRelation.columnName shouldBe "userid"
    simpleRelation.operator shouldBe Operator.Equals
    simpleRelation.term shouldBe Constant("01234567-0123-0123-0123-0123456789ab")

  }

  it should "parse delete specific column statement" in {
    val statement = parseQuery("DELETE phone FROM Users WHERE userid IN (123, 222);").asInstanceOf[DeleteStatement]
    statement.simpleSelection.nonEmpty shouldBe true
    statement.simpleSelection.size shouldBe 1
    statement.simpleSelection(0).asInstanceOf[ColumnName].columnName shouldBe "phone"

    statement.from.table shouldBe "Users"
    statement.using.isEmpty shouldBe true

    val relations = statement.where.relations
    relations.size shouldBe 1
    val simpleRelation = relations(0).asInstanceOf[Simple]
    simpleRelation.columnName shouldBe "userid"
    simpleRelation.operator shouldBe Operator.In
    val literal: TupleLiteral = simpleRelation.term.asInstanceOf[TupleLiteral]
    literal.values.size shouldBe 2
    literal.values(0) shouldBe Constant("123")
    literal.values(1) shouldBe Constant("222")

  }

  it should "parse delete specific columns statement " in {
    val statement = parseQuery("DELETE phone, name FROM Users WHERE userid = 123;").asInstanceOf[DeleteStatement]
    statement.simpleSelection.nonEmpty shouldBe true
    statement.simpleSelection.size shouldBe 2
    statement.simpleSelection(0).asInstanceOf[ColumnName].columnName shouldBe "phone"
    statement.simpleSelection(1).asInstanceOf[ColumnName].columnName shouldBe "name"

    statement.from.table shouldBe "Users"
    statement.using.isEmpty shouldBe true

    val relations = statement.where.relations
    relations.size shouldBe 1
    val simpleRelation = relations(0).asInstanceOf[Simple]
    simpleRelation.columnName shouldBe "userid"
    simpleRelation.operator shouldBe Operator.Equals
    simpleRelation.term.asInstanceOf[Constant].raw shouldBe "123"

  }

  it should "parse delete specific column with field name statement " in {
    val statement = parseQuery("DELETE address.postcode FROM Users WHERE userid = 123;").asInstanceOf[DeleteStatement]
    statement.simpleSelection.nonEmpty shouldBe true
    statement.simpleSelection.size shouldBe 1
    val simpleSelection = statement.simpleSelection(0).asInstanceOf[ColumnNameDot]
    simpleSelection.columnName shouldBe "address"
    simpleSelection.fieldName shouldBe "postcode"

    statement.from.table shouldBe "Users"
    statement.using.isEmpty shouldBe true

    val relations = statement.where.relations
    relations.size shouldBe 1
    val simpleRelation = relations(0).asInstanceOf[Simple]
    simpleRelation.columnName shouldBe "userid"
    simpleRelation.operator shouldBe Operator.Equals
    simpleRelation.term.asInstanceOf[Constant].raw shouldBe "123"

  }

  it should "parse delete specific column with term statement " in {
    val statement = parseQuery("DELETE address['postcode'] FROM Users WHERE userid = 123;").asInstanceOf[DeleteStatement]
    statement.simpleSelection.nonEmpty shouldBe true
    statement.simpleSelection.size shouldBe 1
    val simpleSelection = statement.simpleSelection(0).asInstanceOf[ColumnNameOf]
    simpleSelection.columnName shouldBe "address"
    simpleSelection.term.asInstanceOf[Constant].raw shouldBe "postcode"

    statement.from.table shouldBe "Users"
    statement.using.isEmpty shouldBe true

    val relations = statement.where.relations
    relations.size shouldBe 1
    val simpleRelation = relations(0).asInstanceOf[Simple]
    simpleRelation.columnName shouldBe "userid"
    simpleRelation.operator shouldBe Operator.Equals
    simpleRelation.term.asInstanceOf[Constant].raw shouldBe "123"
  }

  it should "parse delete specific column if exists statement " in {
    val statement = parseQuery("DELETE phone FROM Users WHERE userid = 123 IF EXISTS;").asInstanceOf[DeleteStatement]
    statement.simpleSelection.nonEmpty shouldBe true
    statement.simpleSelection.size shouldBe 1
    statement.simpleSelection(0).asInstanceOf[ColumnName].columnName shouldBe "phone"

    statement.from.table shouldBe "Users"
    statement.using.isEmpty shouldBe true
    statement.ifCondition.isDefined shouldBe true
    statement.ifCondition.get shouldBe IfExist

    val relations = statement.where.relations
    relations.size shouldBe 1
    val simpleRelation = relations(0).asInstanceOf[Simple]
    simpleRelation.columnName shouldBe "userid"
    simpleRelation.operator shouldBe Operator.Equals
    simpleRelation.term.asInstanceOf[Constant].raw shouldBe "123"

  }

  it should "parse delete specific column with simple if condition " in {
    val statement = parseQuery("DELETE phone FROM Users WHERE userid = 123 IF postcode = 'M1' ;").asInstanceOf[DeleteStatement]
    statement.simpleSelection.nonEmpty shouldBe true
    statement.simpleSelection.size shouldBe 1
    statement.simpleSelection(0).asInstanceOf[ColumnName].columnName shouldBe "phone"

    statement.from.table shouldBe "Users"
    statement.using.isEmpty shouldBe true

    statement.ifCondition.isDefined shouldBe true
    val conditions = statement.ifCondition.get.asInstanceOf[IfCondition].conditions
    conditions.size shouldBe 1
    val condition = conditions(0).asInstanceOf[Condition]
    condition.simpleSelection.asInstanceOf[ColumnName].columnName shouldBe "postcode"
    condition.operator shouldBe Operator.Equals
    condition.term.asInstanceOf[Constant].raw shouldBe "M1"

    val relations = statement.where.relations
    relations.size shouldBe 1
    val simpleRelation = relations(0).asInstanceOf[Simple]
    simpleRelation.columnName shouldBe "userid"
    simpleRelation.operator shouldBe Operator.Equals
    simpleRelation.term.asInstanceOf[Constant].raw shouldBe "123"

  }

  it should "parse IN tuple of UUID" ignore {
    val statement = parseQuery("DELETE phone FROM Users WHERE userid IN (C73DE1D3, B70DE1D0);").asInstanceOf[DeleteStatement]
    statement.simpleSelection.nonEmpty shouldBe true
    statement.simpleSelection.size shouldBe 1
    statement.simpleSelection(0).asInstanceOf[ColumnName].columnName shouldBe "phone"

    statement.from.table shouldBe "Users"
    statement.using.isEmpty shouldBe true

    val relations = statement.where.relations
    relations.size shouldBe 1
    val simpleRelation = relations(0).asInstanceOf[Simple]
    simpleRelation.columnName shouldBe "userid"
    simpleRelation.operator shouldBe Operator.In
    val literal: TupleLiteral = simpleRelation.term.asInstanceOf[TupleLiteral]
    literal.values.size shouldBe 2
    literal.values(0) shouldBe Constant("C73DE1D3")
    literal.values(1) shouldBe Constant("B70DE1D0")

  }
}