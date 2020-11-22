/**
 * Generated by Scrooge
 *   version: 18.4.0
 *   rev: b64bcb47af2451b2e51a1ed1b3876f6c06c642b3
 *   built at: 20180410-144307
 */
package xed.leaderboard.domain.thrift

import com.twitter.io.Buf
import com.twitter.scrooge.{
  LazyTProtocol,
  TFieldBlob,
  ThriftException,
  ThriftStruct,
  ThriftStructCodec3,
  ThriftStructFieldInfo,
  ThriftStructMetaData,
  ThriftUtil,
  ValidatingThriftStruct,
  ValidatingThriftStructCodec3
}
import org.apache.thrift.protocol._
import org.apache.thrift.transport.{TMemoryBuffer, TTransport, TIOStreamTransport}
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.util.Arrays
import java.util.concurrent.atomic.AtomicInteger
import scala.collection.immutable.{Map => immutable$Map}
import scala.collection.mutable.Builder
import scala.collection.mutable.{
  ArrayBuffer => mutable$ArrayBuffer, Buffer => mutable$Buffer,
  HashMap => mutable$HashMap, HashSet => mutable$HashSet}
import scala.collection.{Map, Set}


object TPointInfo extends ValidatingThriftStructCodec3[TPointInfo] {
  val NoPassthroughFields: immutable$Map[Short, TFieldBlob] = immutable$Map.empty[Short, TFieldBlob]
  val Struct = new TStruct("TPointInfo")
  val PointField = new TField("point", TType.I32, 1)
  val PointFieldManifest = implicitly[Manifest[Int]]
  val StartTimeField = new TField("startTime", TType.I64, 2)
  val StartTimeFieldManifest = implicitly[Manifest[Long]]
  val DurationField = new TField("duration", TType.I64, 3)
  val DurationFieldManifest = implicitly[Manifest[Long]]
  val ExtraDataField = new TField("extraData", TType.STRING, 4)
  val ExtraDataFieldManifest = implicitly[Manifest[String]]

  /**
   * Field information in declaration order.
   */
  lazy val fieldInfos: scala.List[ThriftStructFieldInfo] = scala.List[ThriftStructFieldInfo](
    new ThriftStructFieldInfo(
      PointField,
      false,
      true,
      PointFieldManifest,
      _root_.scala.None,
      _root_.scala.None,
      immutable$Map.empty[String, String],
      immutable$Map.empty[String, String],
      None
    ),
    new ThriftStructFieldInfo(
      StartTimeField,
      false,
      true,
      StartTimeFieldManifest,
      _root_.scala.None,
      _root_.scala.None,
      immutable$Map.empty[String, String],
      immutable$Map.empty[String, String],
      None
    ),
    new ThriftStructFieldInfo(
      DurationField,
      false,
      true,
      DurationFieldManifest,
      _root_.scala.None,
      _root_.scala.None,
      immutable$Map.empty[String, String],
      immutable$Map.empty[String, String],
      None
    ),
    new ThriftStructFieldInfo(
      ExtraDataField,
      true,
      false,
      ExtraDataFieldManifest,
      _root_.scala.None,
      _root_.scala.None,
      immutable$Map.empty[String, String],
      immutable$Map.empty[String, String],
      None
    )
  )

  lazy val structAnnotations: immutable$Map[String, String] =
    immutable$Map.empty[String, String]

  /**
   * Checks that all required fields are non-null.
   */
  def validate(_item: TPointInfo): Unit = {
  }

  /**
   * Checks that the struct is a valid as a new instance. If there are any missing required or
   * construction required fields, return a non-empty list.
   */
  def validateNewInstance(item: TPointInfo): scala.Seq[com.twitter.scrooge.validation.Issue] = {
    val buf = scala.collection.mutable.ListBuffer.empty[com.twitter.scrooge.validation.Issue]

    buf ++= validateField(item.point)
    buf ++= validateField(item.startTime)
    buf ++= validateField(item.duration)
    buf ++= validateField(item.extraData)
    buf.toList
  }

  def withoutPassthroughFields(original: TPointInfo): TPointInfo =
    new Immutable(
      point =
        {
          val field = original.point
          field
        },
      startTime =
        {
          val field = original.startTime
          field
        },
      duration =
        {
          val field = original.duration
          field
        },
      extraData =
        {
          val field = original.extraData
          field.map { field =>
            field
          }
        }
    )

  override def encode(_item: TPointInfo, _oproto: TProtocol): Unit = {
    _item.write(_oproto)
  }


  private[this] def lazyDecode(_iprot: LazyTProtocol): TPointInfo = {

    var point: Int = 0
    var _got_point = false
    var startTime: Long = 0L
    var _got_startTime = false
    var duration: Long = 0L
    var _got_duration = false
    var extraDataOffset: Int = -1

    var _passthroughFields: Builder[(Short, TFieldBlob), immutable$Map[Short, TFieldBlob]] = null
    var _done = false
    val _start_offset = _iprot.offset

    _iprot.readStructBegin()
    while (!_done) {
      val _field = _iprot.readFieldBegin()
      if (_field.`type` == TType.STOP) {
        _done = true
      } else {
        _field.id match {
          case 1 =>
            _field.`type` match {
              case TType.I32 =>
    
                point = readPointValue(_iprot)
                _got_point = true
              case _actualType =>
                val _expectedType = TType.I32
                throw new TProtocolException(
                  "Received wrong type for field 'point' (expected=%s, actual=%s).".format(
                    ttypeToString(_expectedType),
                    ttypeToString(_actualType)
                  )
                )
            }
          case 2 =>
            _field.`type` match {
              case TType.I64 =>
    
                startTime = readStartTimeValue(_iprot)
                _got_startTime = true
              case _actualType =>
                val _expectedType = TType.I64
                throw new TProtocolException(
                  "Received wrong type for field 'startTime' (expected=%s, actual=%s).".format(
                    ttypeToString(_expectedType),
                    ttypeToString(_actualType)
                  )
                )
            }
          case 3 =>
            _field.`type` match {
              case TType.I64 =>
    
                duration = readDurationValue(_iprot)
                _got_duration = true
              case _actualType =>
                val _expectedType = TType.I64
                throw new TProtocolException(
                  "Received wrong type for field 'duration' (expected=%s, actual=%s).".format(
                    ttypeToString(_expectedType),
                    ttypeToString(_actualType)
                  )
                )
            }
          case 4 =>
            _field.`type` match {
              case TType.STRING =>
                extraDataOffset = _iprot.offsetSkipString
    
              case _actualType =>
                val _expectedType = TType.STRING
                throw new TProtocolException(
                  "Received wrong type for field 'extraData' (expected=%s, actual=%s).".format(
                    ttypeToString(_expectedType),
                    ttypeToString(_actualType)
                  )
                )
            }
          case _ =>
            if (_passthroughFields == null)
              _passthroughFields = immutable$Map.newBuilder[Short, TFieldBlob]
            _passthroughFields += (_field.id -> TFieldBlob.read(_field, _iprot))
        }
        _iprot.readFieldEnd()
      }
    }
    _iprot.readStructEnd()

    if (!_got_point) throw new TProtocolException("Required field 'point' was not found in serialized data for struct TPointInfo")
    if (!_got_startTime) throw new TProtocolException("Required field 'startTime' was not found in serialized data for struct TPointInfo")
    if (!_got_duration) throw new TProtocolException("Required field 'duration' was not found in serialized data for struct TPointInfo")
    new LazyImmutable(
      _iprot,
      _iprot.buffer,
      _start_offset,
      _iprot.offset,
      point,
      startTime,
      duration,
      extraDataOffset,
      if (_passthroughFields == null)
        NoPassthroughFields
      else
        _passthroughFields.result()
    )
  }

  override def decode(_iprot: TProtocol): TPointInfo =
    _iprot match {
      case i: LazyTProtocol => lazyDecode(i)
      case i => eagerDecode(i)
    }

  private[thrift] def eagerDecode(_iprot: TProtocol): TPointInfo = {
    var point: Int = 0
    var _got_point = false
    var startTime: Long = 0L
    var _got_startTime = false
    var duration: Long = 0L
    var _got_duration = false
    var extraData: _root_.scala.Option[String] = _root_.scala.None
    var _passthroughFields: Builder[(Short, TFieldBlob), immutable$Map[Short, TFieldBlob]] = null
    var _done = false

    _iprot.readStructBegin()
    while (!_done) {
      val _field = _iprot.readFieldBegin()
      if (_field.`type` == TType.STOP) {
        _done = true
      } else {
        _field.id match {
          case 1 =>
            _field.`type` match {
              case TType.I32 =>
                point = readPointValue(_iprot)
                _got_point = true
              case _actualType =>
                val _expectedType = TType.I32
                throw new TProtocolException(
                  "Received wrong type for field 'point' (expected=%s, actual=%s).".format(
                    ttypeToString(_expectedType),
                    ttypeToString(_actualType)
                  )
                )
            }
          case 2 =>
            _field.`type` match {
              case TType.I64 =>
                startTime = readStartTimeValue(_iprot)
                _got_startTime = true
              case _actualType =>
                val _expectedType = TType.I64
                throw new TProtocolException(
                  "Received wrong type for field 'startTime' (expected=%s, actual=%s).".format(
                    ttypeToString(_expectedType),
                    ttypeToString(_actualType)
                  )
                )
            }
          case 3 =>
            _field.`type` match {
              case TType.I64 =>
                duration = readDurationValue(_iprot)
                _got_duration = true
              case _actualType =>
                val _expectedType = TType.I64
                throw new TProtocolException(
                  "Received wrong type for field 'duration' (expected=%s, actual=%s).".format(
                    ttypeToString(_expectedType),
                    ttypeToString(_actualType)
                  )
                )
            }
          case 4 =>
            _field.`type` match {
              case TType.STRING =>
                extraData = _root_.scala.Some(readExtraDataValue(_iprot))
              case _actualType =>
                val _expectedType = TType.STRING
                throw new TProtocolException(
                  "Received wrong type for field 'extraData' (expected=%s, actual=%s).".format(
                    ttypeToString(_expectedType),
                    ttypeToString(_actualType)
                  )
                )
            }
          case _ =>
            if (_passthroughFields == null)
              _passthroughFields = immutable$Map.newBuilder[Short, TFieldBlob]
            _passthroughFields += (_field.id -> TFieldBlob.read(_field, _iprot))
        }
        _iprot.readFieldEnd()
      }
    }
    _iprot.readStructEnd()

    if (!_got_point) throw new TProtocolException("Required field 'point' was not found in serialized data for struct TPointInfo")
    if (!_got_startTime) throw new TProtocolException("Required field 'startTime' was not found in serialized data for struct TPointInfo")
    if (!_got_duration) throw new TProtocolException("Required field 'duration' was not found in serialized data for struct TPointInfo")
    new Immutable(
      point,
      startTime,
      duration,
      extraData,
      if (_passthroughFields == null)
        NoPassthroughFields
      else
        _passthroughFields.result()
    )
  }

  def apply(
    point: Int,
    startTime: Long,
    duration: Long,
    extraData: _root_.scala.Option[String] = _root_.scala.None
  ): TPointInfo =
    new Immutable(
      point,
      startTime,
      duration,
      extraData
    )

  def unapply(_item: TPointInfo): _root_.scala.Option[_root_.scala.Tuple4[Int, Long, Long, Option[String]]] = _root_.scala.Some(_item.toTuple)


  @inline private[thrift] def readPointValue(_iprot: TProtocol): Int = {
    _iprot.readI32()
  }

  @inline private def writePointField(point_item: Int, _oprot: TProtocol): Unit = {
    _oprot.writeFieldBegin(PointField)
    writePointValue(point_item, _oprot)
    _oprot.writeFieldEnd()
  }

  @inline private def writePointValue(point_item: Int, _oprot: TProtocol): Unit = {
    _oprot.writeI32(point_item)
  }

  @inline private[thrift] def readStartTimeValue(_iprot: TProtocol): Long = {
    _iprot.readI64()
  }

  @inline private def writeStartTimeField(startTime_item: Long, _oprot: TProtocol): Unit = {
    _oprot.writeFieldBegin(StartTimeField)
    writeStartTimeValue(startTime_item, _oprot)
    _oprot.writeFieldEnd()
  }

  @inline private def writeStartTimeValue(startTime_item: Long, _oprot: TProtocol): Unit = {
    _oprot.writeI64(startTime_item)
  }

  @inline private[thrift] def readDurationValue(_iprot: TProtocol): Long = {
    _iprot.readI64()
  }

  @inline private def writeDurationField(duration_item: Long, _oprot: TProtocol): Unit = {
    _oprot.writeFieldBegin(DurationField)
    writeDurationValue(duration_item, _oprot)
    _oprot.writeFieldEnd()
  }

  @inline private def writeDurationValue(duration_item: Long, _oprot: TProtocol): Unit = {
    _oprot.writeI64(duration_item)
  }

  @inline private[thrift] def readExtraDataValue(_iprot: TProtocol): String = {
    _iprot.readString()
  }

  @inline private def writeExtraDataField(extraData_item: String, _oprot: TProtocol): Unit = {
    _oprot.writeFieldBegin(ExtraDataField)
    writeExtraDataValue(extraData_item, _oprot)
    _oprot.writeFieldEnd()
  }

  @inline private def writeExtraDataValue(extraData_item: String, _oprot: TProtocol): Unit = {
    _oprot.writeString(extraData_item)
  }


  object Immutable extends ThriftStructCodec3[TPointInfo] {
    override def encode(_item: TPointInfo, _oproto: TProtocol): Unit = { _item.write(_oproto) }
    override def decode(_iprot: TProtocol): TPointInfo = TPointInfo.decode(_iprot)
    override lazy val metaData: ThriftStructMetaData[TPointInfo] = TPointInfo.metaData
  }

  /**
   * The default read-only implementation of TPointInfo.  You typically should not need to
   * directly reference this class; instead, use the TPointInfo.apply method to construct
   * new instances.
   */
  class Immutable(
      val point: Int,
      val startTime: Long,
      val duration: Long,
      val extraData: _root_.scala.Option[String],
      override val _passthroughFields: immutable$Map[Short, TFieldBlob])
    extends TPointInfo {
    def this(
      point: Int,
      startTime: Long,
      duration: Long,
      extraData: _root_.scala.Option[String] = _root_.scala.None
    ) = this(
      point,
      startTime,
      duration,
      extraData,
      Map.empty[Short, TFieldBlob]
    )
  }

  /**
   * This is another Immutable, this however keeps strings as lazy values that are lazily decoded from the backing
   * array byte on read.
   */
  private[this] class LazyImmutable(
      _proto: LazyTProtocol,
      _buf: Array[Byte],
      _start_offset: Int,
      _end_offset: Int,
      val point: Int,
      val startTime: Long,
      val duration: Long,
      extraDataOffset: Int,
      override val _passthroughFields: immutable$Map[Short, TFieldBlob])
    extends TPointInfo {

    override def write(_oprot: TProtocol): Unit = {
      _oprot match {
        case i: LazyTProtocol => i.writeRaw(_buf, _start_offset, _end_offset - _start_offset)
        case _ => super.write(_oprot)
      }
    }

    lazy val extraData: _root_.scala.Option[String] =
      if (extraDataOffset == -1)
        None
      else {
        Some(_proto.decodeString(_buf, extraDataOffset))
      }

    /**
     * Override the super hash code to make it a lazy val rather than def.
     *
     * Calculating the hash code can be expensive, caching it where possible
     * can provide significant performance wins. (Key in a hash map for instance)
     * Usually not safe since the normal constructor will accept a mutable map or
     * set as an arg
     * Here however we control how the class is generated from serialized data.
     * With the class private and the contract that we throw away our mutable references
     * having the hash code lazy here is safe.
     */
    override lazy val hashCode = super.hashCode
  }

  /**
   * This Proxy trait allows you to extend the TPointInfo trait with additional state or
   * behavior and implement the read-only methods from TPointInfo using an underlying
   * instance.
   */
  trait Proxy extends TPointInfo {
    protected def _underlying_TPointInfo: TPointInfo
    override def point: Int = _underlying_TPointInfo.point
    override def startTime: Long = _underlying_TPointInfo.startTime
    override def duration: Long = _underlying_TPointInfo.duration
    override def extraData: _root_.scala.Option[String] = _underlying_TPointInfo.extraData
    override def _passthroughFields = _underlying_TPointInfo._passthroughFields
  }
}

/**
 * Prefer the companion object's [[xed.leaderboard.domain.thrift.TPointInfo.apply]]
 * for construction if you don't need to specify passthrough fields.
 */
trait TPointInfo
  extends ThriftStruct
  with _root_.scala.Product4[Int, Long, Long, Option[String]]
  with ValidatingThriftStruct[TPointInfo]
  with java.io.Serializable
{
  import TPointInfo._

  def point: Int
  def startTime: Long
  def duration: Long
  def extraData: _root_.scala.Option[String]

  def _passthroughFields: immutable$Map[Short, TFieldBlob] = immutable$Map.empty

  def _1 = point
  def _2 = startTime
  def _3 = duration
  def _4 = extraData

  def toTuple: _root_.scala.Tuple4[Int, Long, Long, Option[String]] = {
    (
      point,
      startTime,
      duration,
      extraData
    )
  }


  /**
   * Gets a field value encoded as a binary blob using TCompactProtocol.  If the specified field
   * is present in the passthrough map, that value is returned.  Otherwise, if the specified field
   * is known and not optional and set to None, then the field is serialized and returned.
   */
  def getFieldBlob(_fieldId: Short): _root_.scala.Option[TFieldBlob] = {
    lazy val _buff = new TMemoryBuffer(32)
    lazy val _oprot = new TCompactProtocol(_buff)
    _passthroughFields.get(_fieldId) match {
      case blob: _root_.scala.Some[TFieldBlob] => blob
      case _root_.scala.None => {
        val _fieldOpt: _root_.scala.Option[TField] =
          _fieldId match {
            case 1 =>
              if (true) {
                writePointValue(point, _oprot)
                _root_.scala.Some(TPointInfo.PointField)
              } else {
                _root_.scala.None
              }
            case 2 =>
              if (true) {
                writeStartTimeValue(startTime, _oprot)
                _root_.scala.Some(TPointInfo.StartTimeField)
              } else {
                _root_.scala.None
              }
            case 3 =>
              if (true) {
                writeDurationValue(duration, _oprot)
                _root_.scala.Some(TPointInfo.DurationField)
              } else {
                _root_.scala.None
              }
            case 4 =>
              if (extraData.isDefined) {
                writeExtraDataValue(extraData.get, _oprot)
                _root_.scala.Some(TPointInfo.ExtraDataField)
              } else {
                _root_.scala.None
              }
            case _ => _root_.scala.None
          }
        _fieldOpt match {
          case _root_.scala.Some(_field) =>
            _root_.scala.Some(TFieldBlob(_field, Buf.ByteArray.Owned(_buff.getArray())))
          case _root_.scala.None =>
            _root_.scala.None
        }
      }
    }
  }

  /**
   * Collects TCompactProtocol-encoded field values according to `getFieldBlob` into a map.
   */
  def getFieldBlobs(ids: TraversableOnce[Short]): immutable$Map[Short, TFieldBlob] =
    (ids flatMap { id => getFieldBlob(id) map { id -> _ } }).toMap

  /**
   * Sets a field using a TCompactProtocol-encoded binary blob.  If the field is a known
   * field, the blob is decoded and the field is set to the decoded value.  If the field
   * is unknown and passthrough fields are enabled, then the blob will be stored in
   * _passthroughFields.
   */
  def setField(_blob: TFieldBlob): TPointInfo = {
    var point: Int = this.point
    var startTime: Long = this.startTime
    var duration: Long = this.duration
    var extraData: _root_.scala.Option[String] = this.extraData
    var _passthroughFields = this._passthroughFields
    _blob.id match {
      case 1 =>
        point = readPointValue(_blob.read)
      case 2 =>
        startTime = readStartTimeValue(_blob.read)
      case 3 =>
        duration = readDurationValue(_blob.read)
      case 4 =>
        extraData = _root_.scala.Some(readExtraDataValue(_blob.read))
      case _ => _passthroughFields += (_blob.id -> _blob)
    }
    new Immutable(
      point,
      startTime,
      duration,
      extraData,
      _passthroughFields
    )
  }

  /**
   * If the specified field is optional, it is set to None.  Otherwise, if the field is
   * known, it is reverted to its default value; if the field is unknown, it is removed
   * from the passthroughFields map, if present.
   */
  def unsetField(_fieldId: Short): TPointInfo = {
    var point: Int = this.point
    var startTime: Long = this.startTime
    var duration: Long = this.duration
    var extraData: _root_.scala.Option[String] = this.extraData

    _fieldId match {
      case 1 =>
        point = 0
      case 2 =>
        startTime = 0L
      case 3 =>
        duration = 0L
      case 4 =>
        extraData = _root_.scala.None
      case _ =>
    }
    new Immutable(
      point,
      startTime,
      duration,
      extraData,
      _passthroughFields - _fieldId
    )
  }

  /**
   * If the specified field is optional, it is set to None.  Otherwise, if the field is
   * known, it is reverted to its default value; if the field is unknown, it is removed
   * from the passthroughFields map, if present.
   */
  def unsetPoint: TPointInfo = unsetField(1)

  def unsetStartTime: TPointInfo = unsetField(2)

  def unsetDuration: TPointInfo = unsetField(3)

  def unsetExtraData: TPointInfo = unsetField(4)


  override def write(_oprot: TProtocol): Unit = {
    TPointInfo.validate(this)
    _oprot.writeStructBegin(Struct)
    writePointField(point, _oprot)
    writeStartTimeField(startTime, _oprot)
    writeDurationField(duration, _oprot)
    if (extraData.isDefined) writeExtraDataField(extraData.get, _oprot)
    if (_passthroughFields.nonEmpty) {
      _passthroughFields.values.foreach { _.write(_oprot) }
    }
    _oprot.writeFieldStop()
    _oprot.writeStructEnd()
  }

  def copy(
    point: Int = this.point,
    startTime: Long = this.startTime,
    duration: Long = this.duration,
    extraData: _root_.scala.Option[String] = this.extraData,
    _passthroughFields: immutable$Map[Short, TFieldBlob] = this._passthroughFields
  ): TPointInfo =
    new Immutable(
      point,
      startTime,
      duration,
      extraData,
      _passthroughFields
    )

  override def canEqual(other: Any): Boolean = other.isInstanceOf[TPointInfo]

  private def _equals(x: TPointInfo, y: TPointInfo): Boolean =
      x.productArity == y.productArity &&
      x.productIterator.sameElements(y.productIterator)

  override def equals(other: Any): Boolean =
    canEqual(other) &&
      _equals(this, other.asInstanceOf[TPointInfo]) &&
      _passthroughFields == other.asInstanceOf[TPointInfo]._passthroughFields

  override def hashCode: Int = _root_.scala.runtime.ScalaRunTime._hashCode(this)

  override def toString: String = _root_.scala.runtime.ScalaRunTime._toString(this)


  override def productArity: Int = 4

  override def productElement(n: Int): Any = n match {
    case 0 => this.point
    case 1 => this.startTime
    case 2 => this.duration
    case 3 => this.extraData
    case _ => throw new IndexOutOfBoundsException(n.toString)
  }

  override def productPrefix: String = "TPointInfo"

  def _codec: ValidatingThriftStructCodec3[TPointInfo] = TPointInfo
}
