package xed.api_service.domain.design

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.{DeserializationContext, JsonDeserializer, JsonNode}
import xed.api_service.domain.design
import xed.api_service.domain.design.v100.{FillInBlank, MultiChoice, MultiSelect}
import xed.api_service.util.JsonUtils

object ComponentType {
  val Text = "text"
  val Image = "image"
  val Audio = "audio"
  val Video = "video"
  val InactiveMultiChoice = "inactive_multi_choice"
  val MultiChoice = "multi_choice"
  val MultiSelect = "multi_select"
  val FillInBlank = "fill_in_blank"
  val Dictionary = "dictionary"
  val Answer = "answer"
  val Panel = "panel"
}

object CardVersion {
  val V100 = 100
}

@SerialVersionUID(20200602L)
@JsonDeserialize(using = classOf[ComponentDeserializer])
abstract class Component extends Serializable {
  val componentType: String
}

abstract class Container extends Component {
  val components: Seq[Component]
  val isHorizontal: Option[Boolean]
  val backgroundColor: Option[BgColorConfig]
  val alignment: Option[Int]


  def hasNoAction() = !hasAction()

  def hasAction() =  countActionComponent() > 0

  def countActionComponent(): Int = getActionComponents().size

  @JsonIgnore
  def getActionComponent() = getActionComponents().headOption

  @JsonIgnore
  def getActionComponents() = {
    Option(components)
      .getOrElse(Seq.empty)
      .filter({
        case _: FillInBlank | _: MultiChoice | _: MultiSelect => true
        case _ => false
      })
  }

}


class ComponentDeserializer extends JsonDeserializer[Component] {

  override def deserialize(p: JsonParser, ctx: DeserializationContext): Component = {
    val rootNode = p.getCodec.readTree[JsonNode](p)

    if (rootNode.isMissingNode) return null

    val version = rootNode.path("version").asInt(CardVersion.V100)
    val componentType = rootNode.path("component_type").asText()

    version match {
      case CardVersion.V100 => buildComponentV100(componentType,rootNode)
      case _ => null
    }

  }

  private def buildComponentV100(componentType: String, rootNode: JsonNode) = {
    componentType match {
      case ComponentType.Text => JsonUtils.fromJson[design.v100.Text](rootNode.toString)
      case ComponentType.Image => JsonUtils.fromJson[design.v100.Image](rootNode.toString)
      case ComponentType.Audio => JsonUtils.fromJson[design.v100.Audio](rootNode.toString)
      case ComponentType.Video => JsonUtils.fromJson[design.v100.Video](rootNode.toString)
      case ComponentType.Answer => JsonUtils.fromJson[design.v100.Answer](rootNode.toString)
      case ComponentType.InactiveMultiChoice => JsonUtils.fromJson[design.v100.InactiveMultiChoice](rootNode.toString)
      case ComponentType.MultiChoice => JsonUtils.fromJson[design.v100.MultiChoice](rootNode.toString)
      case ComponentType.MultiSelect => JsonUtils.fromJson[design.v100.MultiSelect](rootNode.toString)
      case ComponentType.FillInBlank => JsonUtils.fromJson[design.v100.FillInBlank](rootNode.toString)
      case ComponentType.Dictionary => JsonUtils.fromJson[design.v100.Dictionary](rootNode.toString)
      case ComponentType.Panel => JsonUtils.fromJson[design.v100.Panel](rootNode.toString)
      case _ => null
    }
  }
}
