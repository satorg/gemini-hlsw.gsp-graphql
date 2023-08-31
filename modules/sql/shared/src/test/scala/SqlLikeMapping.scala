// Copyright (c) 2016-2020 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package edu.gemini.grackle.sql.test

import edu.gemini.grackle._
import edu.gemini.grackle.syntax._
import Query._, Predicate._, Value._
import QueryCompiler._
import sql.Like

trait SqlLikeMapping[F[_]] extends SqlTestMapping[F] {

  object likes extends TableDef("likes") {
    val id = col("id", int4)
    val notNullableText = col("notnullable", text)
    val nullableText = col("nullable", nullable(text))
  }

  val schema =
    schema"""
      type Query {
        likes: [Like!]!
        likeNotNullableNotNullable(pattern: String!): [Like!]!
        likeNotNullableNullable(pattern: String): [Like!]!
        likeNullableNotNullable(pattern: String!): [Like!]!
        likeNullableNullable(pattern: String): [Like!]!
      }
      type Like {
        id: Int!
        notNullable: String!
        nullable: String
      }
    """

  val QueryType = schema.ref("Query")
  val LikeType = schema.ref("Like")

  val typeMappings =
    List(
      ObjectMapping(
        tpe = QueryType,
        fieldMappings =
          List(
            SqlObject("likes"),
            SqlObject("likeNotNullableNotNullable"),
            SqlObject("likeNotNullableNullable"),
            SqlObject("likeNullableNotNullable"),
            SqlObject("likeNullableNullable")
          )
      ),
      ObjectMapping(
        tpe = LikeType,
        fieldMappings =
          List(
            SqlField("id", likes.id, key = true),
            SqlField("notNullable", likes.notNullableText),
            SqlField("nullable", likes.nullableText)
          )
      )
    )

  object NonNullablePattern {
    def unapply(value: Value): Option[String] =
      value match {
        case AbsentValue => None
        case StringValue(p) => Some(p)
        case _ => None
      }
  }

  object NullablePattern {
    def unapply(value: Value): Option[Option[String]] =
      value match {
        case AbsentValue => None
        case NullValue => Some(None)
        case StringValue(p) => Some(Some(p))
        case _ => None
      }
  }

  def mkPredicate(t: Term[Option[String]], pattern: Option[String]): Predicate =
    pattern match {
      case None => IsNull(t, true)
      case Some(p) => Like(t, p, false)
    }

  override val selectElaborator = SelectElaborator {
    case (QueryType, "likeNotNullableNotNullable", List(Binding("pattern", NonNullablePattern(pattern)))) =>
      Elab.transformChild(child => Filter(Like(LikeType / "notNullable", pattern, false), child))

    case (QueryType, "likeNotNullableNullable", List(Binding("pattern", NullablePattern(pattern)))) =>
      Elab.transformChild(child => Filter(mkPredicate(LikeType / "notNullable", pattern), child))

    case (QueryType, "likeNullableNotNullable", List(Binding("pattern", NonNullablePattern(pattern)))) =>
      Elab.transformChild(child => Filter(Like(LikeType / "nullable", pattern, false), child))

    case (QueryType, "likeNullableNullable", List(Binding("pattern", NullablePattern(pattern)))) =>
      Elab.transformChild(child => Filter(mkPredicate(LikeType / "nullable", pattern), child))
  }
}
