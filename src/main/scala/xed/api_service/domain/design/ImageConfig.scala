package xed.api_service.domain.design

/**
 * @author andy
 * @since 6/3/20
 **/
@SerialVersionUID(20200602L)
case class ImageConfig(scale: Option[Double],
                       rotate: Option[Double],
                       positionX: Option[Double],
                       positionY: Option[Double],
                       rotationFocusX: Option[Double],
                       rotationFocusY: Option[Double],
                       height: Option[Double]) extends Serializable

