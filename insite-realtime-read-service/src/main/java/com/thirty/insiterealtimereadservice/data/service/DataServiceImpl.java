package com.thirty.insiterealtimereadservice.data.service;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import com.influxdb.query.dsl.Flux;
import com.influxdb.query.dsl.functions.restriction.Restrictions;
import com.thirty.insiterealtimereadservice.data.dto.ReferrerDto;
import com.thirty.insiterealtimereadservice.data.dto.UserCountDto;
import com.thirty.insiterealtimereadservice.data.dto.response.AbnormalResDto;
import com.thirty.insiterealtimereadservice.data.dto.response.ReferrerResDto;
import com.thirty.insiterealtimereadservice.data.dto.response.ResponseTimeResDto;
import com.thirty.insiterealtimereadservice.data.dto.response.UserCountResDto;
import com.thirty.insiterealtimereadservice.feignclient.MemberServiceClient;
import com.thirty.insiterealtimereadservice.feignclient.dto.request.MemberValidReqDto;
import feign.FeignException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import javax.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataServiceImpl implements DataService{

    private final MemberServiceClient memberServiceClient;

    @Resource
    private InfluxDBClient influxDBClient;

    @Override
    public ResponseTimeResDto getResponseTime(int memberId, String token) {

        try{
            memberServiceClient.validationMemberAndApplication(memberId, MemberValidReqDto.create(token));
        }catch (FeignException fe){
            log.error(fe.getMessage());
        }

        QueryApi queryApi = influxDBClient.getQueryApi();
        Restrictions restrictions = Restrictions.and(
            Restrictions.measurement().equal("data"),
            Restrictions.tag("applicationToken").equal(token)
        );
        Flux query = Flux.from("insite")
            .range(0L)
            .filter(restrictions)
            .pivot(new String[]{"_time"},new String[]{"_field"},"_value")
            .yield();
        log.info("query= {}",query);

        List<FluxTable> tables = queryApi.query(query.toString());
        double sum = 0.0;
        double size = 0.0;
        for (FluxTable fluxTable : tables) {
            List<FluxRecord> records = fluxTable.getRecords();
            size= records.size();
            for (FluxRecord record : records) {
                String responseTime = record.getValueByKey("responseTime").toString();
                int time = Integer.valueOf(responseTime);

                sum+= time;
            }
        }
        double avg = sum/size;
        return ResponseTimeResDto.create(avg);
    }

    @Override
    public ReferrerResDto getReferrer(int memberId, String token) {
        try{
            memberServiceClient.validationMemberAndApplication(memberId, MemberValidReqDto.create(token));
        }catch (FeignException fe){
            log.error(fe.getMessage());
        }

        QueryApi queryApi = influxDBClient.getQueryApi();
        Restrictions restrictions = Restrictions.and(
            Restrictions.measurement().equal("data"),
            Restrictions.tag("applicationToken").equal(token)
        );
        Flux query = Flux.from("insite")
            .range(0L)
            .filter(restrictions)
            .groupBy("beforeUrl")
            .count();

        log.info("query= {}",query);

        List<FluxTable> tables = queryApi.query(query.toString());
        Map<String, Double> map= new HashMap<>();
        double sum = 0.0;
        for (FluxTable fluxTable : tables) {
            List<FluxRecord> records = fluxTable.getRecords();
            for (FluxRecord record : records) {
                String url = record.getValueByKey("beforeUrl").toString();
                String count = record.getValueByKey("_value").toString();
                Double urlCount = Double.valueOf(count);
                sum += urlCount;

                map.put(url, urlCount);
            }
        }
        List<ReferrerDto> referrerDtoList = new ArrayList<>();
        for(String url : map.keySet()){
            double curCount = map.get(url);

            referrerDtoList.add(ReferrerDto.create(
                url,
                (int)curCount,
                curCount/sum
            ));
        }
        return ReferrerResDto.create(referrerDtoList);
    }

    @Override
    public UserCountResDto getUserCount(int memberId, String token) {
        try{
            memberServiceClient.validationMemberAndApplication(memberId, MemberValidReqDto.create(token));
        }catch (FeignException fe){
            log.error(fe.getMessage());
        }

        QueryApi queryApi = influxDBClient.getQueryApi();
        Restrictions restrictions = Restrictions.and(
            Restrictions.measurement().equal("data"),
            Restrictions.tag("applicationToken").equal(token)
        );
        Flux query = Flux.from("insite")
            .range(0L)
            .filter(restrictions)
            .groupBy("currentUrl")
            .count();

        log.info("query= {}",query);

        List<FluxTable> tables = queryApi.query(query.toString());
        Map<String, Double> map= new HashMap<>();
        double sum = 0.0;
        for (FluxTable fluxTable : tables) {
            List<FluxRecord> records = fluxTable.getRecords();
            for (FluxRecord record : records) {
                String url = record.getValueByKey("currentUrl").toString();
                String count = record.getValueByKey("_value").toString();
                Double urlCount = Double.valueOf(count);
                sum += urlCount;

                map.put(url, urlCount);
            }
        }
        List<UserCountDto> userCountDtoList = new ArrayList<>();
        for(String url : map.keySet()){
            double curCount = map.get(url);

            userCountDtoList.add(UserCountDto.create(
                url,
                (int)curCount,
                curCount/sum
            ));
        }
        return UserCountResDto.create(userCountDtoList);
    }

    @Override
    public AbnormalResDto getAbnormal(int memberId, String token) {
        try{
            memberServiceClient.validationMemberAndApplication(memberId, MemberValidReqDto.create(token));
        }catch (FeignException fe){
            log.error(fe.getMessage());
        }

        QueryApi queryApi = influxDBClient.getQueryApi();
        Restrictions restrictions = Restrictions.and(
            Restrictions.measurement().equal("abnormal"),
            Restrictions.tag("applicationToken").equal(token)
        );
        Flux query = Flux.from("insite")
            .range(0L)
            .filter(restrictions)
            .pivot(new String[]{"_time"}, new String[]{"_field"},"isRead")
            .sort(new String[]{"_time"});

        log.info("query= {}",query);

        List<FluxTable> tables = queryApi.query(query.toString());
        Stack<String> stack= new Stack<>();
        for (FluxTable fluxTable : tables) {
            List<FluxRecord> records = fluxTable.getRecords();
            for (FluxRecord record : records) {
                String isRead = record.getValueByKey("createTime").toString();
                log.info(isRead);
                stack.push(isRead);
            }
        }
        if(stack.pop().equals("true")){
            return AbnormalResDto.create(true);
        }
        return AbnormalResDto.create(false);
    }
}
