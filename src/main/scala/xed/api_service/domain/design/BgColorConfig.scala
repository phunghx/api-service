package xed.api_service.domain.design

/**
 * @author andy
 * @since 6/3/20
 **/
@SerialVersionUID(20200602L)
case class BgColorConfig(colors: Seq[Int],
                         stops: Seq[Double],
                         begin: Int,
                         end: Int) extends Serializable

