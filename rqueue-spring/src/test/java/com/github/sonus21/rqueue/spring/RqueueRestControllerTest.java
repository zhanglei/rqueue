/*
 * Copyright 2020 Sonu Kumar
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.sonus21.rqueue.spring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.sonus21.rqueue.common.RqueueRedisTemplate;
import com.github.sonus21.rqueue.core.RqueueMessage;
import com.github.sonus21.rqueue.core.RqueueMessageSender;
import com.github.sonus21.rqueue.core.RqueueMessageTemplate;
import com.github.sonus21.rqueue.exception.TimedOutException;
import com.github.sonus21.rqueue.models.db.MessageMetadata;
import com.github.sonus21.rqueue.models.enums.ActionType;
import com.github.sonus21.rqueue.models.enums.AggregationType;
import com.github.sonus21.rqueue.models.enums.ChartType;
import com.github.sonus21.rqueue.models.enums.DataType;
import com.github.sonus21.rqueue.models.request.ChartDataRequest;
import com.github.sonus21.rqueue.models.request.MessageMoveRequest;
import com.github.sonus21.rqueue.models.response.BaseResponse;
import com.github.sonus21.rqueue.models.response.BooleanResponse;
import com.github.sonus21.rqueue.models.response.ChartDataResponse;
import com.github.sonus21.rqueue.models.response.DataViewResponse;
import com.github.sonus21.rqueue.models.response.MessageMoveResponse;
import com.github.sonus21.rqueue.models.response.StringResponse;
import com.github.sonus21.rqueue.spring.app.AppWithMetricEnabled;
import com.github.sonus21.rqueue.spring.app.AppWithMetricEnabled.DeleteMessageListener;
import com.github.sonus21.rqueue.test.dto.Email;
import com.github.sonus21.rqueue.test.dto.Job;
import com.github.sonus21.rqueue.utils.Constants;
import com.github.sonus21.rqueue.utils.SystemUtils;
import com.github.sonus21.rqueue.utils.TimeoutUtils;
import com.github.sonus21.test.RqueueSpringTestRunner;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@ContextConfiguration(classes = AppWithMetricEnabled.class)
@RunWith(RqueueSpringTestRunner.class)
@Slf4j
@WebAppConfiguration
@TestPropertySource(
    properties = {
      "spring.redis.port=6386",
      "mysql.db.name=RqueueRestController",
      "max.workers.count=40",
      "notification.queue.active=false",
    })
public class RqueueRestControllerTest {
  @Autowired private RqueueMessageSender rqueueMessageSender;
  @Autowired private WebApplicationContext wac;
  @Autowired private DeleteMessageListener deleteMessageListener;
  @Autowired private RqueueMessageTemplate rqueueMessageTemplate;

  @Value("${job.queue.name}")
  private String jobQueueName;

  @Value("${notification.queue.name}")
  private String notificationQueue;

  @Value("${email.queue.name}")
  private String emailQueueName;

  @Value("${email.dead.letter.queue.name}")
  private String emailDlq;

  private MockMvc mockMvc;
  private ObjectMapper mapper = new ObjectMapper();

  @Autowired private RqueueRedisTemplate<String> stringRqueueRedisTemplate;

  @Before
  public void init() throws TimedOutException {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
  }

  @Test
  public void testGetChartLatency() throws Exception {
    for (int i = 0; i < 100; i++) {
      Job job = Job.newInstance();
      rqueueMessageSender.enqueue(jobQueueName, job);
    }
    TimeoutUtils.waitFor(
        () -> rqueueMessageSender.getAllMessages(jobQueueName).size() == 0,
        Constants.SECONDS_IN_A_MINUTE * Constants.ONE_MILLI,
        "Job to run");
    ChartDataRequest chartDataRequest =
        new ChartDataRequest(ChartType.LATENCY, AggregationType.DAILY);
    MvcResult result =
        this.mockMvc
            .perform(
                post("/rqueue/api/v1/chart")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsBytes(chartDataRequest)))
            .andReturn();
    String response = result.getResponse().getContentAsString();
    ChartDataResponse dataResponse = mapper.readValue(response, ChartDataResponse.class);
    assertNull(dataResponse.getMessage());
    assertEquals(0, dataResponse.getCode());
    assertEquals(181, dataResponse.getData().size());
  }

  @Test
  public void testGetChartStats() throws Exception {
    for (int i = 0; i < 100; i++) {
      Job job = Job.newInstance();
      rqueueMessageSender.enqueue(jobQueueName, job);
    }
    TimeoutUtils.waitFor(
        () -> rqueueMessageSender.getAllMessages(jobQueueName).size() == 0,
        Constants.SECONDS_IN_A_MINUTE * Constants.ONE_MILLI,
        "Job to run");
    ChartDataRequest chartDataRequest =
        new ChartDataRequest(ChartType.STATS, AggregationType.DAILY);
    MvcResult result =
        this.mockMvc
            .perform(
                post("/rqueue/api/v1/chart")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsBytes(chartDataRequest)))
            .andReturn();
    String response = result.getResponse().getContentAsString();
    ChartDataResponse dataResponse = mapper.readValue(response, ChartDataResponse.class);
    assertNull(dataResponse.getMessage());
    assertEquals(0, dataResponse.getCode());
    assertEquals(181, dataResponse.getData().size());
  }

  @Test
  public void testExploreDataList() throws Exception {
    for (int i = 0; i < 30; i++) {
      rqueueMessageSender.enqueue(emailDlq, Email.newInstance());
    }
    MvcResult result =
        this.mockMvc
            .perform(
                get("/rqueue/api/v1/explore")
                    .param("type", "LIST")
                    .param("src", emailQueueName)
                    .param("name", emailDlq))
            .andReturn();

    DataViewResponse dataViewResponse =
        mapper.readValue(result.getResponse().getContentAsString(), DataViewResponse.class);
    assertNull(dataViewResponse.getMessage());
    assertEquals(0, dataViewResponse.getCode());
    assertEquals(Collections.singletonList(ActionType.DELETE), dataViewResponse.getActions());
    assertEquals(20, dataViewResponse.getRows().size());
    assertEquals(3, dataViewResponse.getRows().get(0).size());
  }

  @Test
  public void testExploreDataZset() throws Exception {
    for (int i = 0; i < 30; i++) {
      rqueueMessageSender.enqueueIn(
          emailQueueName, Email.newInstance(), Constants.SECONDS_IN_A_MINUTE * Constants.ONE_MILLI);
    }
    MvcResult result =
        this.mockMvc
            .perform(
                get("/rqueue/api/v1/explore")
                    .param("type", "ZSET")
                    .param("src", emailQueueName)
                    .param("name", "_rq::" + emailQueueName))
            .andReturn();

    DataViewResponse dataViewResponse =
        mapper.readValue(result.getResponse().getContentAsString(), DataViewResponse.class);
    assertNull(dataViewResponse.getMessage());
    assertEquals(0, dataViewResponse.getCode());
    assertEquals(Collections.singletonList(ActionType.NONE), dataViewResponse.getActions());
    assertEquals(20, dataViewResponse.getRows().size());
    assertEquals(4, dataViewResponse.getRows().get(0).size());
  }

  @Test
  public void deleteDataSet() throws Exception {
    for (int i = 0; i < 30; i++) {
      rqueueMessageSender.enqueue(emailDlq, Email.newInstance());
    }
    MvcResult result =
        this.mockMvc.perform(delete("/rqueue/api/v1/data-set/" + emailDlq)).andReturn();
    BooleanResponse booleanResponse =
        mapper.readValue(result.getResponse().getContentAsString(), BooleanResponse.class);
    assertNull(booleanResponse.getMessage());
    assertEquals(0, booleanResponse.getCode());
    assertTrue(booleanResponse.isValue());
    assertFalse(stringRqueueRedisTemplate.exist(emailDlq));
    assertEquals(-2, stringRqueueRedisTemplate.ttl(emailDlq));
  }

  @Test
  public void dataType() throws Exception {
    rqueueMessageSender.enqueue(emailDlq, Email.newInstance());
    MvcResult result =
        this.mockMvc.perform(get("/rqueue/api/v1/data-type").param("name", emailDlq)).andReturn();
    StringResponse response =
        mapper.readValue(result.getResponse().getContentAsString(), StringResponse.class);
    assertNull(response.getMessage());
    assertEquals(0, response.getCode());
    assertEquals("LIST", response.getVal());
  }

  @Test
  public void moveMessage() throws Exception {
    for (int i = 0; i < 30; i++) {
      rqueueMessageSender.enqueue(emailDlq, Email.newInstance());
    }
    MessageMoveRequest request =
        new MessageMoveRequest(emailDlq, DataType.LIST, emailQueueName, DataType.LIST);
    MvcResult result =
        this.mockMvc
            .perform(
                put("/rqueue/api/v1/move")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(request)))
            .andReturn();
    MessageMoveResponse response =
        mapper.readValue(result.getResponse().getContentAsString(), MessageMoveResponse.class);
    assertNull(response.getMessage());
    assertEquals(0, response.getCode());
    assertEquals(100, response.getNumberOfMessageTransferred());
    assertEquals(Long.valueOf(0), stringRqueueRedisTemplate.getListSize(emailDlq));
  }

  @Test
  public void viewData() throws Exception {
    for (int i = 0; i < 30; i++) {
      rqueueMessageSender.enqueue(emailDlq, Email.newInstance());
    }
    MvcResult result =
        this.mockMvc
            .perform(
                get("/rqueue/api/v1/data")
                    .param("name", emailDlq)
                    .param("type", DataType.LIST.name())
                    .contentType(MediaType.APPLICATION_JSON))
            .andReturn();
    DataViewResponse dataViewResponse =
        mapper.readValue(result.getResponse().getContentAsString(), DataViewResponse.class);
    assertEquals(0, dataViewResponse.getCode());
    assertEquals(Collections.emptyList(), dataViewResponse.getActions());
    assertEquals(20, dataViewResponse.getRows().size());
  }

  @Test
  public void deleteQueue() throws Exception {
    for (int i = 0; i < 30; i++) {
      rqueueMessageSender.enqueue(jobQueueName, Job.newInstance());
    }
    MvcResult result =
        this.mockMvc
            .perform(
                delete("/rqueue/api/v1/queues/" + jobQueueName)
                    .contentType(MediaType.APPLICATION_JSON))
            .andReturn();
    BaseResponse response =
        mapper.readValue(result.getResponse().getContentAsString(), BaseResponse.class);
    assertEquals(0, response.getCode());
    assertEquals("Queue deleted", response.getMessage());
  }

  @Test
  public void deleteMessage() throws Exception {
    Email job = Email.newInstance();
    deleteMessageListener.clear();
    rqueueMessageSender.enqueueIn(emailQueueName, job, 10 * Constants.ONE_MILLI);
//    RqueueMessage message =
//        rqueueMessageTemplate
//            .readFromZset(SystemUtils.getDelayedQueueName(emailQueueName), 0, -1)
//            .get(0);
//    rqueueMessageSender.enqueueIn(
//        jobQueueName, job, Constants.SECONDS_IN_A_MINUTE * Constants.ONE_MILLI);
//    MvcResult result =
//        this.mockMvc
//            .perform(
//                delete("/rqueue/api/v1/data-set/" + jobQueueName + "/" + job.getId())
//                    .contentType(MediaType.APPLICATION_JSON))
//            .andReturn();
//    BooleanResponse response =
//        mapper.readValue(result.getResponse().getContentAsString(), BooleanResponse.class);
//    assertEquals(0, response.getCode());
//    Object metadata =
//        stringRqueueRedisTemplate.get(SystemUtils.getMessageMetadataKey(message.getId()));
//    assertTrue(((MessageMetadata) metadata).isDeleted());
    TimeoutUtils.waitFor(
        () -> {
          List<Object> messages = deleteMessageListener.getMessages();
          return messages.size() == 1 && job.equals(messages.get(0));
        },
        30 * Constants.ONE_MILLI,
        "message to be deleted");
  }
}