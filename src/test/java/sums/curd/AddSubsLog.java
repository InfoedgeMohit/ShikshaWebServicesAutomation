package sums.curd;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import common.Common;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

public class AddSubsLog extends Common {

	static String apiPath;
	String api;
	static String[] params;
	static String[] paramType;
	Response apiResponse = null;
	String apiResponseMsgExpected;
	static String testDataFilePath;

	static ArrayList<Integer> responseTimes = new ArrayList<Integer>();

	@BeforeClass
	public void doItBeforeTest() {
		apiResponseExpectedTime = 1000; // in MS
		// api path
		apiPath = "sums/v1/curd/addSubsLog";
		api = serverPath + apiPath;
		// API params in sequence followed by apiResponseMsgExpected
		params = new String[]{"subsId","consumeQuant","clientId","sumsId","baseProductId","consumedId","consumedType","startDate","endDate","apiResponseMsgExpected"};
		// type of API params in sequence
		paramType = new String[]{"Integer","Integer","Integer","Integer","Integer","Integer","String","Date","Date"};
		// test data file
		testDataFilePath = "//src//test/resources//sums//curd//AddSubsLog.xlsx";
	}

	@DataProvider
	public static String[][] testData() throws Exception {
		// The number of times data is repeated, test will be executed the same no. of times
		String[][] testData = prepareTestData(params, testDataFilePath);
		return testData;
	}

	@Test(dataProvider = "testData", priority = 1)
	public void verifyAddSubsLogApi(String subsId, String consumeQuant, String clientId, String sumsId, String baseProductId, String consumedId, String consumedType, 
			String startDate, String endDate, String apiResponseMsgExpected) {
		this.apiResponseMsgExpected = apiResponseMsgExpected;

		// pass api params and hit api
		if(subsId.equals("ignoreInput") && consumeQuant.equals("ignoreInput") && clientId.equals("ignoreInput") && sumsId.equals("ignoreInput") && baseProductId.equals
				("ignoreInput") && consumedId.equals("ignoreInput") && consumedType.equals("ignoreInput") && startDate.equals("ignoreInput") && endDate.equals("ignoreInput"))
			apiResponse = RestAssured.given().when().post(api).then().extract().response();
		else if(!subsId.equals("ignoreInput") && consumeQuant.equals("ignoreInput") && clientId.equals("ignoreInput") && sumsId.equals("ignoreInput") && baseProductId.equals
				("ignoreInput") && consumedId.equals("ignoreInput") && consumedType.equals("ignoreInput") && startDate.equals("ignoreInput") && endDate.equals("ignoreInput"))
			apiResponse = RestAssured.given().
				param("subsId",subsId).
			when().post(api).then().extract().response();
		else if(!subsId.equals("ignoreInput") && !consumeQuant.equals("ignoreInput") && clientId.equals("ignoreInput") && sumsId.equals("ignoreInput") && baseProductId.equals
				("ignoreInput") && consumedId.equals("ignoreInput") && consumedType.equals("ignoreInput") && startDate.equals("ignoreInput") && endDate.equals("ignoreInput"))
			apiResponse = RestAssured.given().
				param("subsId",subsId).
				param("consumeQuant",consumeQuant).
			when().post(api).then().extract().response();
		else if(!subsId.equals("ignoreInput") && !consumeQuant.equals("ignoreInput") && !clientId.equals("ignoreInput") && sumsId.equals("ignoreInput") && baseProductId.equals
				("ignoreInput") && consumedId.equals("ignoreInput") && consumedType.equals("ignoreInput") && startDate.equals("ignoreInput") && endDate.equals("ignoreInput"))
			apiResponse = RestAssured.given().
				param("subsId",subsId).
				param("consumeQuant",consumeQuant).
				param("clientId",clientId).
			when().post(api).then().extract().response();
		else if(!subsId.equals("ignoreInput") && !consumeQuant.equals("ignoreInput") && !clientId.equals("ignoreInput") && !sumsId.equals("ignoreInput") && baseProductId.equals
				("ignoreInput") && consumedId.equals("ignoreInput") && consumedType.equals("ignoreInput") && startDate.equals("ignoreInput") && endDate.equals("ignoreInput"))
			apiResponse = RestAssured.given().
				param("subsId",subsId).
				param("consumeQuant",consumeQuant).
				param("clientId",clientId).
				param("sumsId",sumsId).
			when().post(api).then().extract().response();
		else if(!subsId.equals("ignoreInput") && !consumeQuant.equals("ignoreInput") && !clientId.equals("ignoreInput") && !sumsId.equals("ignoreInput") && !baseProductId.equals
				("ignoreInput") && consumedId.equals("ignoreInput") && consumedType.equals("ignoreInput") && startDate.equals("ignoreInput") && endDate.equals("ignoreInput"))
			apiResponse = RestAssured.given().
				param("subsId",subsId).
				param("consumeQuant",consumeQuant).
				param("clientId",clientId).
				param("sumsId",sumsId).
				param("baseProductId",baseProductId).
			when().post(api).then().extract().response();
		else if(!subsId.equals("ignoreInput") && !consumeQuant.equals("ignoreInput") && !clientId.equals("ignoreInput") && !sumsId.equals("ignoreInput") && !baseProductId.equals
				("ignoreInput") && !consumedId.equals("ignoreInput") && consumedType.equals("ignoreInput") && startDate.equals("ignoreInput") && endDate.equals("ignoreInput"))
			apiResponse = RestAssured.given().
				param("subsId",subsId).
				param("consumeQuant",consumeQuant).
				param("clientId",clientId).
				param("sumsId",sumsId).
				param("baseProductId",baseProductId).
				param("consumedId",consumedId).
			when().post(api).then().extract().response();
		else if(!subsId.equals("ignoreInput") && !consumeQuant.equals("ignoreInput") && !clientId.equals("ignoreInput") && !sumsId.equals("ignoreInput") && !baseProductId.equals
				("ignoreInput") && !consumedId.equals("ignoreInput") && !consumedType.equals("ignoreInput") && startDate.equals("ignoreInput") && endDate.equals("ignoreInput"))
			apiResponse = RestAssured.given().
				param("subsId",subsId).
				param("consumeQuant",consumeQuant).
				param("clientId",clientId).
				param("sumsId",sumsId).
				param("baseProductId",baseProductId).
				param("consumedId",consumedId).
				param("consumedType",consumedType).
			when().post(api).then().extract().response();
		else if(!subsId.equals("ignoreInput") && !consumeQuant.equals("ignoreInput") && !clientId.equals("ignoreInput") && !sumsId.equals("ignoreInput") && !baseProductId.equals
				("ignoreInput") && !consumedId.equals("ignoreInput") && !consumedType.equals("ignoreInput") && !startDate.equals("ignoreInput") && endDate.equals("ignoreInput"))
			apiResponse = RestAssured.given().
				param("subsId",subsId).
				param("consumeQuant",consumeQuant).
				param("clientId",clientId).
				param("sumsId",sumsId).
				param("baseProductId",baseProductId).
				param("consumedId",consumedId).
				param("consumedType",consumedType).
				param("startDate",startDate).
			when().post(api).then().extract().response();
		else
			apiResponse = RestAssured.given().
				param("subsId",subsId).
				param("consumeQuant",consumeQuant).
				param("clientId",clientId).
				param("sumsId",sumsId).
				param("baseProductId",baseProductId).
				param("consumedId",consumedId).
				param("consumedType",consumedType).
				param("startDate",startDate).
				param("endDate",endDate).
			when().post(api).then().extract().response();

		// fetch status code from response
		int statusCode = apiResponse.getStatusCode();
		JsonPath jsonPath = apiResponse.jsonPath();

		if(statusCode == 200){
			// In Positive cases, verify values of status, message and data [from Db]
			// Reporter.log("Positive case", true);
			verifyPostiveCases(subsId, consumeQuant, clientId, sumsId, baseProductId, consumedId, consumedType, startDate, endDate, jsonPath);
		} else if(statusCode == 400){
			// In Negative cases, verify values of param[field], paramType[code] and apiResponseMsg [message]
			// Reporter.log("Negative case", true);
			verifyNegativeCases(params, jsonPath, paramType, apiResponseMsgExpected);
		} else{
			Reporter.log(", InCorrect Response Code : "+statusCode, true);
			Assert.fail("InCorrect Response Code : "+statusCode);
		}

		long apiResponseActualTime = apiResponse.getTimeIn(TimeUnit.MILLISECONDS);
		responseTimes.add((int)apiResponseActualTime);

		if(apiResponseActualTime > apiResponseExpectedTime){
			Reporter.log("<br> API Response time : "+apiResponseActualTime+" MS is greater than expected time ("+apiResponseExpectedTime+" MS)", true);
			Assert.fail("API Response time : "+apiResponseActualTime+" MS is greater than expected time ("+apiResponseExpectedTime+" MS)");
		}else {
			// Reporter.log("<br> API Response time : "+apiResponseActualTime+" MS", true);
		}
	}

	public void verifyPostiveCases(String subsId, String consumeQuant, String clientId, String sumsId, String baseProductId, String consumedId, String consumedType, 
			String startDate, String endDate, JsonPath jsonPath){
		try {
			// verify status is success
			String statusResponse = jsonPath.getString("status");
			if(statusResponse.contentEquals("success")) {
				// Reporter.log(", Correct status [success]", true);
			}
			else {
				Reporter.log(", InCorrect status ["+statusResponse+"]", true);
				Assert.fail("InCorrect status ["+statusResponse+"]");
			}

			// verify message is null
			String messageResponse = jsonPath.getString("message");
			if(messageResponse == null) {
				// Reporter.log(", Correct message [null]", true);
			}
			else{
				Reporter.log(", InCorrect message ["+messageResponse+"]", true);
				Assert.fail("InCorrect message ["+messageResponse+"]");
			}

			// verify value of data ["result" / "logId" // "ERROR"]
			// fetch SubscriptionLogId from SubscriptionLog table
			ResultSet rs = exceuteDbQuery("SELECT SubscriptionLogId FROM SubscriptionLog where ClientUserId = "+clientId+" and SumsUserId = "+sumsId+" and "
					+ "SubscriptionId = "+subsId+" and ConsumedBaseProductId = "+baseProductId+" and ConsumedId = "+consumedId+" and ConsumedIdType = '"+consumedType+"' and "
							+ "NumberConsumed = "+consumeQuant+" and ConsumptionStartDate = '"+startDate+"' and ConsumptionEndDate = '"+endDate+"' and "
									+ "ABS(TIME_TO_SEC(TIMEDIFF(now(), ConsumptionTime))) < 100 order by SubscriptionLogId desc limit 1;","SUMS");

			// get row count of resultSet of sql query
			int rowcount = getCountOfResultSet(rs);
			if(rowcount > 0){
				rs.next();
				int subscriptionLogId = rs.getInt("SubscriptionLogId");
				// Reporter.log(", Correct entry captured in SubscriptionLog [SubscriptionLogId : "+subscriptionLogId+"]", true);
				// verify value of data in response as {result:null, logId:<logId>, ERROR:1}
				String dataResponse = jsonPath.getString("data");
				String resultDataResponse = jsonPath.getString("data.result");
				String logIdDataResponse = jsonPath.getString("data.logId");
				String errorDataResponse = jsonPath.getString("data.ERROR");
				if(resultDataResponse == null && Integer.parseInt(logIdDataResponse) == subscriptionLogId && errorDataResponse == null) {
					// Reporter.log(", Correct value of data [result:null, logId:"+subscriptionLogId+", ERROR:null]", true);
				}
				else{
					Reporter.log(", InCorrect value of data ["+dataResponse+"]", true);
					Assert.fail("InCorrect value of data ["+dataResponse+"]");
				}
			}else{
				Reporter.log(", No entry done in SubscriptionLog", true);
				Assert.fail("No entry done in SubscriptionLog");
			}
		}catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@Test (priority = 2)
	public static void respnseTimeStats_AddSubsLog(){
		respnseTimeStatsCommon(responseTimes, apiPath.substring(apiPath.lastIndexOf("/")+1));
	}
}