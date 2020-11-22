package xed.api_service.service.course

import com.fasterxml.jackson.databind.node.TextNode
import com.google.inject.Inject
import com.twitter.util.Future
import xed.api_service.domain.course.CategoryInfo
import xed.api_service.domain.exception.{InternalError, NotFoundError}
import xed.api_service.domain.request._
import xed.api_service.domain.response.PageResult
import xed.api_service.repository.CategoryRepository
import xed.api_service.util.ConcurrentUtils
import xed.userprofile.SignedInUser

import scala.collection.mutable.ListBuffer

trait CategoryService {
  def create(request: CreateXCategoryRequest): Future[CategoryInfo]

  def delete(user: SignedInUser, id: String): Future[Boolean]

  def update(user: SignedInUser, request: UpdateXCategoryRequest): Future[Boolean]

  def get(id: String): Future[Option[CategoryInfo]]

  def listAll(documentStatus: Option[Int] = None): Future[Seq[CategoryInfo]]

  def search(request: SearchRequest): Future[PageResult[CategoryInfo]]
}

case class CategoryServiceImpl @Inject()(categoryRepository: CategoryRepository) extends CategoryService {

  override def create(request: CreateXCategoryRequest): Future[CategoryInfo] = {
    val categoryInfo = request.build()
    for {
      r <- categoryRepository.insert(categoryInfo,false)
    } yield r.isDefined match {
      case true => categoryInfo
      case _ => throw InternalError(Some(s"Can't create category."))
    }
  }


  override def delete(user: SignedInUser, id: String): Future[Boolean] = {
    for {
      r <- categoryRepository.delete(id)
    } yield r
  }

  override def update(user: SignedInUser, request: UpdateXCategoryRequest): Future[Boolean] = {

    for {
      category <- categoryRepository.get(request.id).map(throwIfNotExist(_))
      _ = checkPerms(user,category)
      categoryInfo = CategoryInfo(
        id = request.id,
        name = request.name,
        documentStatus = request.documentStatus,
        updatedTime = Some(System.currentTimeMillis()),
        createdTime = None
      )
      r <- categoryRepository.update(categoryInfo).map(_.count > 0)
    } yield r match {
      case true => r
      case _ => throw InternalError(Some("Can't update this category."))
    }
  }


  override def get(id: String): Future[Option[CategoryInfo]] = {
    categoryRepository.get(id)
  }

  override def listAll(documentStatus: Option[Int]): Future[Seq[CategoryInfo]] = {
    val searchRequest = SearchRequest()
    documentStatus.foreach(status => {
      searchRequest.addIfNotExist(TermsQuery("document_status",ListBuffer(TextNode.valueOf(status.toString))))
    })

    def fn(from: Int, size: Int) =  categoryRepository.genericSearch(searchRequest.copy(from = from, size = size)).map(_.records)

    ConcurrentUtils.getUntilEmpty(Seq.empty[CategoryInfo],fn,0,100)
  }

  override def search(request: SearchRequest): Future[PageResult[CategoryInfo]] = {
    categoryRepository.genericSearch(request)
  }


  private def throwIfNotExist[T](v : Option[T], msg: Option[String] = None) = v match  {
    case Some(x) => x
    case _ => throw NotFoundError(msg)
  }

  private def checkPerms(user: SignedInUser, categoryInfo: CategoryInfo) = {

  }
}
