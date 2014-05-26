/*
 * Copyright 2014 Stratio.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.stratio.streaming.api

import java.util.UUID
import com.stratio.streaming.commons.messages.StratioStreamingMessage
import com.stratio.streaming.commons.exceptions.StratioEngineOperationException
import com.stratio.streaming.kafka.KafkaProducer
import com.stratio.streaming.zookeeper.ZookeeperConsumer
import com.stratio.streaming.commons.constants.REPLY_CODES._
import com.google.gson.Gson
import com.stratio.streaming.commons.dto.ActionCallbackDto

case class StreamingAPISyncOperation(
  kafkaProducer: KafkaProducer,
  zookeeperConsumer: ZookeeperConsumer)
  extends StreamingAPIOperation {

  /**
   * Sends the message to the StratioStreamingEngine and waits
   * for the Acknowledge to be written in zookeeper.
   *
   * @param message
   */
  def performSyncOperation(message: StratioStreamingMessage) = {
    val zNodeUniqueId = UUID.randomUUID().toString
    addMessageToKafkaTopic(message, zNodeUniqueId, kafkaProducer)
    val syncOperationResponse = waitForTheStreamingResponse(zookeeperConsumer, message)
    manageStreamingResponse(syncOperationResponse, message)
  }

  private def manageStreamingResponse(response: String, message: StratioStreamingMessage) = {
    val responseDto = new Gson().fromJson(response, classOf[ActionCallbackDto])
    val replyCode = responseDto.getErrorCode
    replyCode match {
      case OK => log.info("StratioEngine Ack received for: "+message.getRequest)
      case _ => {
        createLogError(replyCode, responseDto.getDescription)
        throw new StratioEngineOperationException("StratioEngine error: "+responseDto.getDescription)
      }
    }
  }

  private def createLogError(responseCode: Int, queryString: String) = {
    log.error(s"StratioAPI - [ACK_CODE,QUERY_STRING]: [$responseCode,$queryString]")
  }
}
