package xed.api_service.util.mail

import java.io.{File, FileReader, StringWriter}

import com.github.mustachejava.DefaultMustacheFactory
import com.twitter.finatra.http.internal.marshalling.mustache.ScalaObjectHandler


trait MailFormatter {
  def format(data: Any): String
}

case class MustacheMailFormatter(templateFile: String) extends MailFormatter {
  private val mf = new DefaultMustacheFactory() {
    setObjectHandler(new ScalaObjectHandler)
  }
  private val mustache = {
    val file = new File(templateFile)
    if (!file.isFile) throw new Exception(s"File ${file.getAbsolutePath} not found")
    val reader = new FileReader(file)
    mf.compile(reader, file.toPath.getFileName.toString)
  }

  override def format(data: Any): String = {
    val result = new StringWriter()
    mustache.execute(result, data).flush()
    result.toString
  }
}
