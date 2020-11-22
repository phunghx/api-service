package xed.api_service.domain.request

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node._
import com.twitter.finagle.http.Request
import com.twitter.util.{Return, Try}

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

case class NotQuery(field: String, values: ListBuffer[JsonNode]) {
  @JsonIgnore
  def isSingleValue: Boolean = {
    getParsedValue.length == 1
  }

  @JsonIgnore
  def isValid: Boolean = {
    field != null && field.length > 0 && getParsedValue.nonEmpty
  }

  @JsonIgnore
  def getParsedValue: Seq[Any] = {
    values.map(parseValue).filter(_.nonEmpty).map(_.get.toString)
  }

  private def parseValue(value: JsonNode): Option[Any] = {
    value match {
      case _: NullNode => None
      case s: ShortNode => Some(s.shortValue())
      case i: IntNode => Some(i.intValue())
      case l: LongNode => Some(l.longValue())
      case f: FloatNode => Some(f.floatValue())
      case d: DoubleNode => Some(d.doubleValue())
      case b: BooleanNode => Some(b.booleanValue())
      case str: TextNode => Some(str.textValue())
      case _ => None
    }
  }
}

case class TermsQuery(field: String, values: ListBuffer[JsonNode]) {
  @JsonIgnore
  def isSingleValue: Boolean = {
    getParsedValue.length == 1
  }

  @JsonIgnore
  def isValid: Boolean = {
    field != null && field.length > 0 && getParsedValue.nonEmpty
  }

  @JsonIgnore
  def getParsedValue: Seq[Any] = {
    values.map(parseValue).filter(_.nonEmpty).map(_.get.toString)
  }

  private def parseValue(value: JsonNode): Option[Any] = {
    value match {
      case _: NullNode => None
      case s: ShortNode => Some(s.shortValue())
      case i: IntNode => Some(i.intValue())
      case l: LongNode => Some(l.longValue())
      case f: FloatNode => Some(f.floatValue())
      case d: DoubleNode => Some(d.doubleValue())
      case b: BooleanNode => Some(b.booleanValue())
      case str: TextNode => Some(str.textValue())
      case _ => None
    }
  }
}

case class RangeQuery(field: String,
                      var lowValue: Option[Long],
                      var lowIncluded: Boolean,
                      var highValue: Option[Long],
                      var highIncluded: Boolean) {
  @JsonIgnore
  def isValid: Boolean = {
    field != null && field.nonEmpty && (lowValue.isDefined || highValue.isDefined)
  }
}

case class MatchQuery(field: String,
                      var value: String,
                      defaultOperator: Option[String] = Some("and"),
                      analyzeWildcard: Option[Boolean] = Some(true),
                      allowLeadingWildcard: Option[Boolean] = Some(true)) {
  @JsonIgnore
  def isSingleValue: Boolean = {
    getParsedValue.length == 1
  }

  @JsonIgnore
  def isValid: Boolean = {
    field != null && field.length > 0 && getParsedValue.size == 1
  }

  @JsonIgnore
  def getParsedValue: Seq[Any] = {
    Seq(value)
  }

  @JsonIgnore
  def getDefaultOperator : String = defaultOperator match {
    case Some(v) => v.toLowerCase match {
      case "or" | "and" =>  v.toLowerCase
      case _ =>  "or"
    }
    case _ => "or"
  }

}


object SortQuery {
  val ORDER_ASC = "ASC"
  val ORDER_DESC = "DESC"
}

case class SortQuery(field: String, order: String)

object SearchRequest {
  private val notRegex = "^_not_(.+)$".r
  private val matchRegex = "^_mq_(.+)$".r
  private val gteRegex = "^_gte_(.+)$".r
  private val gtRegex = "^_gt_(.+)$".r
  private val lteRegex = "^_lte_(.+)$".r
  private val ltRegex = "^_lt_(.+)$".r
  private val sortRegex = "^_sort_(.+)$".r
  private val quotedString = """^"([^"]+)"$""".r
  private val integerRegex = "^(\\d+)$".r
  private val doubleRegex = "^[0-9]+(\\.[0-9]+)?$".r

  def parseGetRequest(request: Request): SearchRequest = {
    val params = request.getParams()
    val notTerms = mutable.HashMap.empty[String, NotQuery]
    val terms = mutable.HashMap.empty[String, TermsQuery]
    val ranges = mutable.HashMap.empty[String, RangeQuery]
    val strings = mutable.HashMap.empty[String, MatchQuery]
    val sorts = ListBuffer.empty[SortQuery]
    var from = 0
    var size = 20
    params.foreach(param => {
      param.getKey match {
        case "from" => from = param.getValue.toInt
        case "size" => size = param.getValue.toInt
        case sortRegex(name) => sorts += SortQuery(name, param.getValue)

        case notRegex(name) =>  notTerms.get(name)  match {
          case Some(t) => t.values += parseValue(param.getValue)
          case _ => notTerms.put(name, NotQuery(name, ListBuffer(parseValue(param.getValue))))
        }
        case matchRegex(name) =>  strings.get(name) match {
          case Some(t) => t.value = parseValueAsString(param.getValue)
          case _ => strings.put(name, MatchQuery(name,parseValueAsString(param.getValue) ))
        }
        case gteRegex(name) =>
          ranges.get(name) match {
            case Some(x) =>
              x.lowIncluded = true
              x.lowValue = Some(param.getValue.toLong)
            case _ => ranges.put(name, RangeQuery(name, Some(param.getValue.toLong), true, None, false))
          }
        case gtRegex(name) =>
          ranges.get(name) match {
            case Some(x) =>
              x.lowIncluded = false
              x.lowValue = Some(param.getValue.toLong)
            case _ => ranges.put(name, RangeQuery(name, Some(param.getValue.toLong), false, None, false))
          }

        case lteRegex(name) =>
          ranges.get(name) match {
            case Some(x) =>
              x.highIncluded = true
              x.highValue = Some(param.getValue.toLong)
            case _ => ranges.put(name, RangeQuery(name, None, false, Some(param.getValue.toLong), true))
          }
        case ltRegex(name) =>
          ranges.get(name) match {
            case Some(x) =>
              x.highIncluded = false
              x.highValue = Some(param.getValue.toLong)
            case _ => ranges.put(name, RangeQuery(name, None, false, Some(param.getValue.toLong), false))
          }

        case x => terms.get(x) match {
          case Some(t) => t.values += parseValue(param.getValue)
          case _ => terms.put(x, TermsQuery(x, ListBuffer(parseValue(param.getValue))))
        }
      }
    })
    SearchRequest(
      terms = Some(terms.values.toSeq),
      notTerms = Some(notTerms.values.toSeq),
      ranges = Some(ranges.values.toSeq),
      matches = Some(strings.values.toSeq),
      sorts = Some(sorts),
      from = from,
      size = size
    )
  }


  private def parseValue(value: String): JsonNode = {
    value match {
      case quotedString(v) =>
        TextNode.valueOf(v)
      case integerRegex(n) =>
        Try(n.toLong) match {
          case Return(r) => LongNode.valueOf(r)
          case _ => TextNode.valueOf(n)
        }
      case doubleRegex(_*) =>
        Try(value.toDouble) match {
          case Return(d) => DoubleNode.valueOf(d)
          case _ => TextNode.valueOf(value)
        }
      case _ => TextNode.valueOf(value)
    }
  }

  private def parseValueAsString(value: String): String = {
    value match {
      case quotedString(v) => v
      case integerRegex(n) =>
        Try(n.toLong) match {
          case Return(r) => r.toString
          case _ => n
        }
      case doubleRegex(_*) =>
        Try(value.toDouble) match {
          case Return(d) => d.toString
          case _ => value
        }
      case _ => value
    }
  }

}

case class SearchRequest(
                         terms: Option[Seq[TermsQuery]] = None,
                         notTerms: Option[Seq[NotQuery]] = None,
                         ranges: Option[Seq[RangeQuery]] = None,
                         matches: Option[Seq[MatchQuery]] = None,
                         sorts: Option[Seq[SortQuery]] = None,
                         from: Int = 0,
                         size: Int = 20) {

  def getNotTerms = notTerms.getOrElse(Seq.empty)

  def getTerms = terms.getOrElse(Seq.empty)

  def getRanges = ranges.getOrElse(Seq.empty)

  def getMatches = matches.getOrElse(Seq.empty)

  def getSorts = sorts.getOrElse(Seq.empty)

  def removeField(field: String): SearchRequest = {
    SearchRequest(
      terms = terms.map(_.filter(_.field != field)),
      notTerms = notTerms.map(_.filter(_.field != field)),
      ranges = ranges.map(_.filter(_.field != field)),
      matches = matches.map(_.filter(_.field != field)),
      sorts = sorts.map(_.filter(_.field != field)),
      from = from,
      size = size
    )
  }

  def addIfNotExist(termsQuery: TermsQuery): SearchRequest = {
    if (!this.terms.exists(_.exists(_.field == termsQuery.field))) {
      this.copy(terms = Some(terms.getOrElse(Seq.empty) :+ termsQuery))
    } else {
      this
    }
  }

  def addIfNotExist(notQuery: NotQuery): SearchRequest = {
    if (!this.notTerms.exists(_.exists(_.field == notQuery.field))) {
      this.copy(notTerms = Some(notTerms.getOrElse(Seq.empty) :+ notQuery))
    } else {
      this
    }
  }

  def addIfNotExist(rangeQuery: RangeQuery): SearchRequest = {
    if (!this.ranges.exists(_.exists(_.field == rangeQuery.field))) {
      this.copy(ranges = Some(ranges.getOrElse(Seq.empty) :+ rangeQuery))
    } else {
      this
    }
  }

  def addIfNotExist(stringQuery: MatchQuery): SearchRequest = {
    if (!this.matches.exists(_.exists(_.field == stringQuery.field))) {
      this.copy(matches = Some(matches.getOrElse(Seq.empty) :+ stringQuery))
    } else {
      this
    }
  }

  def addIfNotExist(sortQuery: SortQuery): SearchRequest = {
    if (!this.sorts.exists(_.exists(_.field == sortQuery.field))) {
      this.copy(sorts = Some(sorts.getOrElse(Seq.empty) :+ sortQuery))
    } else {
      this
    }
  }

  def addFirstIfNotExist(sortQuery: SortQuery): SearchRequest = {
    if (!this.sorts.exists(_.exists(_.field == sortQuery.field))) {
      this.copy(sorts = Some(sortQuery +: sorts.getOrElse(Seq.empty)))
    } else {
      this
    }

  }

  def removeSortField(field: String): SearchRequest = {
    this.copy(sorts = this.sorts.map(_.filter(_.field != field)))
  }

  def removeTermsField(field: String): SearchRequest = {
    this.copy(terms = this.terms.map(_.filter(_.field != field)))
  }

  def removeNotTermsField(field: String): SearchRequest = {
    this.copy(notTerms = this.notTerms.map(_.filter(_.field != field)))
  }

  def removeRangeField(field: String): SearchRequest = {
    this.copy(ranges = this.ranges.map(_.filter(_.field != field)))
  }


  def removeStringQField(field: String): SearchRequest = {
    this.copy(matches = this.matches.map(_.filter(_.field != field)))
  }
}