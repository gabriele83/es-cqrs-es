package it.gabfav.es_cqrs_es.read

import akka.persistence.query.TimeBasedUUID
import com.sksamuel.elastic4s.circe._
import com.sksamuel.elastic4s.get.GetRequest
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.{ ElasticClient, ElasticProperties }
import com.sksamuel.elastic4s.indexes.IndexRequest
import io.circe.generic.auto._
import it.gabfav.es_cqrs_es.GlobalConfig._
import it.gabfav.es_cqrs_es.domain.BankAccount.BankAccountEvent
import org.apache.http.auth.{ AuthScope, UsernamePasswordCredentials }
import org.apache.http.client.config.RequestConfig.Builder
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder
import org.elasticsearch.client.RestClientBuilder.{ HttpClientConfigCallback, RequestConfigCallback }

object ESHelper {

  private val elasticDefaultType = "default"
  private val defaultUserPassword = "elastic"
  private val esUser: String = sys.env.getOrElse("ES_USER", defaultUserPassword)
  private val esPassword: String = sys.env.getOrElse("ES_PASSWORD", defaultUserPassword)

  // offset index
  val offsetPrivateIndex: String = s"${esIndexPrefix}_private_offset"
  // offset ids
  val bankAccountOffsetId = "bankAccountOffset"

  // bank account index
  val bankAccountIndex: String = s"${esIndexPrefix}_bank_account"

  private lazy val elasticCredentialsProvider: BasicCredentialsProvider = {
    val credentials = new UsernamePasswordCredentials(esUser, esPassword)
    val provider = new BasicCredentialsProvider
    provider.setCredentials(AuthScope.ANY, credentials)
    provider
  }

  def elasticClient: ElasticClient = ElasticClient(ElasticProperties(esHost), new RequestConfigCallback {
    override def customizeRequestConfig(requestConfigBuilder: Builder): Builder = {
      requestConfigBuilder
    }
  }, new HttpClientConfigCallback {
    override def customizeHttpClient(httpClientBuilder: HttpAsyncClientBuilder): HttpAsyncClientBuilder = {
      httpClientBuilder.setDefaultCredentialsProvider(elasticCredentialsProvider)
    }
  })

  def getOffsetRequest(offsetId: String): GetRequest = get(offsetId)
    .from(offsetPrivateIndex / elasticDefaultType)

  def indexEventsRequest(events: Seq[BankAccountEvent]): Seq[IndexRequest] = events map { evt ⇒
    indexInto(bankAccountIndex / elasticDefaultType).doc(evt)
  }

  def indexOffsetRequest(offsetId: String)(offset: TimeBasedUUID): IndexRequest = indexInto(offsetPrivateIndex / elasticDefaultType)
    .id(offsetId)
    .doc(offset)

}
