package xed.api_service.domain.design

/**
 * @author andy
 * @since 6/3/20
 **/
@SerialVersionUID(20200602L)
case class TextConfig(isUpperCase: Option[Boolean],
                      isUnderline: Option[Boolean],
                      color: Option[Int],
                      fontFamily: Option[String],
                      fontWeight: Option[Int],
                      fontStyle: Option[Int],
                      textAlign: Option[Int],
                      fontSize: Option[Double],
                      letterSpacing: Option[Double],
                      wordSpacing: Option[Double],
                      lineHeight: Option[Double],
                      background: Option[Int],
                      foreground: Option[Int]) extends Serializable

