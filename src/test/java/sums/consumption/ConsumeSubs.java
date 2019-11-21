package sums.consumption;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
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

public class ConsumeSubs extends Common {

	static String apiPath;
	String api;
	static String[] params;
	static String[] paramType;
	Response apiResponse = null;
	String apiResponseMsgExpected;
	static String testDataFilePath;
	String subsId;
	String consumeQuant;
	int baseProdRemainingQuantityDbBefore;

	static ArrayList<Integer> responseTimes = new ArrayList<Integer>();

	@BeforeClass
	public void doItBeforeTest() {
		apiResponseExpectedTime = 1000; // in MS
		// api path
		apiPath = "sums/v1/consume/consumeSubs";
		api = serverPath + apiPath;
		// API params in sequence followed by apiResponseMsgExpected
		params = new String[]{"subsId","consumeQuant","clientId","sumsId","remainQuant","baseProductId","consumedId","consumedType","startDate","endDate","apiResponseMsgExpected"};
		// type of API params in sequence
		paramType = new String[]{"Integer","Integer","Integer","Integer","Integer","Integer","Integer","String","Date","Date"};
		// test data file
		testDataFilePath = "//src//test/resources//sums//consumption//ConsumeSubs.xlsx";
	}

	@DataProvider
	public static String[][] testData() throws Exception {
		// The number of times data is repeated, test will be executed the same no. of times
		String[][] testData = prepareTestData(params, testDataFilePath);
		return testData;
	}

	@Test(dataProvider = "testData", priority = 1)
	public void verifyConsumeSubsApi(String subsId, String consumeQuant, String clientId, String sumsId, String remainQuant, String baseProductId, String consumedId, 
			String consumedType, String startDate, String endDate, String apiResponseMsgExpected) {
		this.apiResponseMsgExpected = apiResponseMsgExpected;
		this.subsId = subsId;
		this.consumeQuant = consumeQuant;

		// pass api params and hit api
		if(subsId.equals("ignoreInput") && consumeQuant.equals("ignoreInput") && clientId.equals("ignoreInput") && sumsId.equals("ignoreInput"))
			apiResponse = RestAssured.given().when().post(api).then().extract().response();
		else if(!subsId.equals("ignoreInput") && consumeQuant.equals("ignoreInput") && clientId.equals("ignoreInput") && sumsId.equals("ignoreInput"))
			apiResponse = RestAssured.given().
			param("subsId",subsId).
			when().post(api).then().extract().response();
		else if(subsId.equals("ignoreInput") && !consumeQuant.equals("ignoreInput") && clientId.equals("ignoreInput") && sumsId.equals("ignoreInput"))
			apiResponse = RestAssured.given().
			param("consumeQuant",consumeQuant).
			when().post(api).then().extract().response();
		else if(!subsId.equals("ignoreInput") && !consumeQuant.equals("ignoreInput") && clientId.equals("ignoreInput") && sumsId.equals("ignoreInput"))
			apiResponse = RestAssured.given().
			param("subsId",subsId).
			param("consumeQuant",consumeQuant).
			when().post(api).then().extract().response();
		else if(!subsId.equals("ignoreInput") && !consumeQuant.equals("ignoreInput") && !clientId.equals("ignoreInput") && sumsId.equals("ignoreInput"))
			apiResponse = RestAssured.given().
			param("subsId",subsId).
			param("consumeQuant",consumeQuant).
			param("clientId",clientId).
			when().post(api).then().extract().response();
		// LDB case (i.e. - consumedType not provided)
		else if(!subsId.equals("ignoreInput") && !consumeQuant.equals("ignoreInput") && !clientId.equals("ignoreInput") && !sumsId.equals("ignoreInput") && 
				consumedType.equals("ignoreInput")){
			// fetch baseProdRemainingQuantityDbBefore from db before hitting api
			if(apiResponseMsgExpected.equalsIgnoreCase("NA"))
				baseProdRemainingQuantityDbBefore = fetchBaseProdRemainingQuantityFromDb();
			apiResponse = RestAssured.given().
					param("subsId",subsId).
					param("consumeQuant",consumeQuant).
					param("clientId",clientId).
					param("sumsId",sumsId).
					param("remainQuant",remainQuant).
					param("baseProductId",baseProductId).
					param("consumedId",consumedId).
					param("startDate",startDate).
					param("endDate",endDate).
					when().post(api).then().extract().response();
		}
		// Non-LDB case (i.e. - consumedType provided)
		else if(!subsId.equals("ignoreInput") && !consumeQuant.equals("ignoreInput") && !clientId.equals("ignoreInput") && !sumsId.equals("ignoreInput") && 
				!consumedType.equals("ignoreInput")){
			// fetch baseProdRemainingQuantityDbBefore from db before hitting api
			if(apiResponseMsgExpected.equalsIgnoreCase("NA"))
				baseProdRemainingQuantityDbBefore = fetchBaseProdRemainingQuantityFromDb();
			apiResponse = RestAssured.given().
					param("subsId",subsId).
					param("consumeQuant",consumeQuant).
					param("clientId",clientId).
					param("sumsId",sumsId).
					param("remainQuant",remainQuant).
					param("baseProductId",baseProductId).
					param("consumedId",consumedId).
					param("consumedType",consumedType).
					param("startDate",startDate).
					param("endDate",endDate).
					when().post(api).then().extract().response();
		}

		// fetch status code from response
		int statusCode = apiResponse.getStatusCode();
		JsonPath jsonPath = apiResponse.jsonPath();

		if(statusCode == 200){
			// In Positive cases, verify values of status, message and data [from Db]
			// Reporter.log("Positive case", true);
			verifyPostiveCases(subsId, consumeQuant, clientId, sumsId, remainQuant, baseProductId, consumedId, consumedType, startDate, endDate, jsonPath);
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

	public void verifyPostiveCases(String subsId, String consumeQuant, String clientId, String sumsId, String remainQuant, String baseProductId, String consumedId, 
			String consumedType, String startDate, String endDate, JsonPath jsonPath){
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
			// fetch details from Subscription_Product_Mapping
			ResultSet rs = exceuteDbQuery("SELECT BaseProdRemainingQuantity, BaseProductId, SubscriptionStartDate, SubscriptionEndDate FROM Subscription_Product_Mapping where status = 'ACTIVE' AND "
					+ "DATE(subscriptionEndDate) >= CURDATE() AND DATE(subscriptionStartDate) <= CURDATE() and SubscriptionId = "+subsId+" ;","SUMS");

			// get row count of resultSet of sql query
			int rowcount = getCountOfResultSet(rs);
			int baseProdRemainingQuantityDbAfter = baseProdRemainingQuantityDbBefore; // assigning as current value
			if(rowcount > 0){
				rs.next();
				baseProdRemainingQuantityDbAfter = rs.getInt("BaseProdRemainingQuantity");
				int consumeQuantInput = Integer.parseInt(consumeQuant);


				// verify value of {result:null, logId:null, ERROR:1} [case3] if baseProdRemainingQuantityDbBefore <= 0 or baseProdRemainingQuantityDbBefore < consumeQuantInput 
				if(baseProdRemainingQuantityDbBefore <= 0 || baseProdRemainingQuantityDbBefore < consumeQuantInput){
					verifyDataInResponse(3, jsonPath, baseProdRemainingQuantityDbAfter, 0, 0);
				}else { // if valid consumedType provided [Non-LDB case], then output will be {result:null, logId:<logId>, ERROR:null} [case2]
					if(consumedType != null && !consumedType.equals("ignoreInput")){
						int remainQuantInput = (remainQuant == "") ? 100 : Integer.parseInt(remainQuant);
						if(remainQuantInput < consumeQuantInput) // verify value of {result:null, logId:null, ERROR:1} [case3] if remainQuantInput < consumeQuantInput
							verifyDataInResponse(3, jsonPath, baseProdRemainingQuantityDbAfter, 0, 0);
						else {	// Prepare attributes from input params/db to fetch subscriptionLogId from SubscriptionLog
							String consumedBaseProductId = (baseProductId != "") ? baseProductId : rs.getString("BaseProductId");

							String subQueryForSubscriptionLog = "";
							if(startDate != "" && endDate != "") // In this case ConsumptionStartDate and ConsumptionEndDate will be same as provided in input
								subQueryForSubscriptionLog = "and ConsumptionStartDate = '"+startDate+"' and ConsumptionEndDate = '"+endDate+"'";
							else if(startDate != "" && endDate == "") // In this case ConsumptionStartDate and ConsumptionEndDate will be same as existing in Subscription_Product_Mapping
								subQueryForSubscriptionLog = "and ConsumptionStartDate = '"+rs.getString("SubscriptionStartDate")+"' and ConsumptionEndDate = "
										+ "'"+rs.getString("SubscriptionEndDate")+"'";
							else // In this case ConsumptionStartDate will be captured as current time stamp and ConsumptionEndDate will be same as existing in Subscription_Product_Mapping
								subQueryForSubscriptionLog = "and ConsumptionEndDate = '"+rs.getString("SubscriptionEndDate")+"'";

							// fetch subscriptionLogId from SubscriptionLog
							int subscriptionLogId = fetchSubscriptionLogIdFromDb(subsId, clientId, sumsId, consumedBaseProductId, consumedId, consumedType, consumeQuant, 
									subQueryForSubscriptionLog); 

							// verify value of {result:null, logId:<logId>, ERROR:null} [case2]
							verifyDataInResponse(2, jsonPath, baseProdRemainingQuantityDbAfter, subscriptionLogId, consumeQuantInput);

							// check update done correctly in 'listings_main' table if consumption type in {course, institute, university, scholarship, notification}
							String[] consumedTypeArr = {"course","institute","university","scholarship","notification"};
							if(Arrays.asList(consumedTypeArr).contains(consumedType))
								verifyUpdateInlistingsMain(subsId, consumedId, consumedType);
						}
					}else //  if valid consumedType not provided [LDB case], then output should be {result:1, logId:null, ERROR:null} [case1]
						verifyDataInResponse(1, jsonPath, baseProdRemainingQuantityDbAfter, 0, consumeQuantInput);
				}
			}else // verify value of {result:null, logId:null, ERROR:1} [case3] if not valid subsId (so no results from db)
				verifyDataInResponse(3, jsonPath, baseProdRemainingQuantityDbAfter, 0, 0);
		}catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@Test (priority = 2)
	public static void respnseTimeStats_ConsumeSubs(){
		respnseTimeStatsCommon(responseTimes, apiPath.substring(apiPath.lastIndexOf("/")+1));
	}

	// fetch BaseProdRemainingQuantity from db before hitting api
	public int fetchBaseProdRemainingQuantityFromDb(){
		int baseProdRemainingQuantityDb = 0;

		ResultSet rs = exceuteDbQuery("SELECT BaseProdRemainingQuantity FROM Subscription_Product_Mapping where status = 'ACTIVE' AND "
				+ "DATE(subscriptionEndDate) >= CURDATE() AND DATE(subscriptionStartDate) <= CURDATE() and SubscriptionId = "+subsId+" ;","SUMS");

		int rowcount = getCountOfResultSet(rs);
		if(rowcount > 0){
			try {
				rs.next();
				baseProdRemainingQuantityDb = rs.getInt("BaseProdRemainingQuantity");
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return baseProdRemainingQuantityDb;
	}

	// fetch SubscriptionLogId from SubscriptionLogId
	public int fetchSubscriptionLogIdFromDb(String subsId, String clientId, String sumsId, String consumedBaseProductId, String consumedId, String consumedType, 
			String consumeQuant, String subQueryForSubscriptionLog){
		int subscriptionLogId = 0;

		ResultSet rs = exceuteDbQuery("SELECT SubscriptionLogId FROM SubscriptionLog where SubscriptionId = "+subsId+" and ClientUserId = "+clientId+" and SumsUserId = "
				+ ""+sumsId+" and ConsumedBaseProductId = "+consumedBaseProductId+" and ConsumedId = "+consumedId+" and ConsumedIdType = '"+consumedType+"' and "
				+ "NumberConsumed = "+consumeQuant+" and ABS(TIME_TO_SEC(TIMEDIFF(now(), ConsumptionTime))) < 100 "+subQueryForSubscriptionLog+" order by SubscriptionLogId desc limit 1;","SUMS");

		int rowcount = getCountOfResultSet(rs);
		if(rowcount > 0){
			try {
				rs.next();
				subscriptionLogId = rs.getInt("SubscriptionLogId");
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return subscriptionLogId;
	}

	// verify different combo of 'data' in api response
	public void verifyDataInResponse(int caseType, JsonPath jsonPath, int baseProdRemainingQuantityDbAfter, int subscriptionLogId, int consumeQuantInput){
		// possible cases of caseType: 
		// 1 : result:1, logId:null, ERROR:1 [LDB case] 
		// 2 : result:null, logId:<logId>, ERROR:1 [Non-LDB case]
		// 3 : result:null, logId:null, ERROR:1 [Error case]
		String dataResponse = jsonPath.getString("data");
		String resultDataResponse = jsonPath.getString("data.result");
		String logIdDataResponse = jsonPath.getString("data.logId");
		String errorDataResponse = jsonPath.getString("data.ERROR");

		switch(caseType){
		// caseType = 1 {result:1, logId:null, ERROR:1 [LDB case]}
		case 1 : {
			if(resultDataResponse.equalsIgnoreCase("1") && logIdDataResponse == null && errorDataResponse == null) {
				// Reporter.log(", Correct value of data [result:1, logId:null, ERROR:null]", true);
			}
			else{
				Reporter.log(", InCorrect value of data ["+dataResponse+"]", true);
				Assert.fail("InCorrect value of data ["+dataResponse+"]");
			}
			// Correct credits should be deducted in db
			if(baseProdRemainingQuantityDbAfter == baseProdRemainingQuantityDbBefore - consumeQuantInput) {
				// Reporter.log(", BaseProdRemainingQuantity["+baseProdRemainingQuantityDbAfter+"] updated correctly in db", true);
			}
			else{
				Reporter.log(", BaseProdRemainingQuantity["+baseProdRemainingQuantityDbAfter+"] not updated correctly in db", true);
				Assert.fail("BaseProdRemainingQuantity["+baseProdRemainingQuantityDbAfter+"] not updated correctly in db");
			}
			break;
		}
		// caseType = 2 {result:null, logId:<logId>, ERROR:1 [Non-LDB case]}
		case 2 : {
			if(resultDataResponse == null && Integer.parseInt(logIdDataResponse) == subscriptionLogId && errorDataResponse == null) {
				// Reporter.log(", Correct value of data [result:null, logId:"+subscriptionLogId+", ERROR:null]", true);
			}
			else{
				Reporter.log(", InCorrect value of data ["+dataResponse+"]", true);
				Assert.fail("InCorrect value of data ["+dataResponse+"]");
			}
			// Correct credits should be deducted in db
			if(baseProdRemainingQuantityDbAfter == baseProdRemainingQuantityDbBefore - consumeQuantInput) {
				// Reporter.log(", BaseProdRemainingQuantity["+baseProdRemainingQuantityDbAfter+"] updated correctly in db", true);
			}
			else{
				Reporter.log(", BaseProdRemainingQuantity["+baseProdRemainingQuantityDbAfter+"] not updated correctly in db", true);
				Assert.fail("BaseProdRemainingQuantity["+baseProdRemainingQuantityDbAfter+"] not updated correctly in db");
			}
			break;
		}
		// caseType = 3 {result:null, logId:null, ERROR:1 [Error case]}
		case 3 : {
			if(resultDataResponse == null && logIdDataResponse == null && errorDataResponse.equalsIgnoreCase("1")) {
				// Reporter.log(", Correct value of data [result:null, logId:null, ERROR:1]", true);
			}
			else{
				Reporter.log(", InCorrect value of data ["+dataResponse+"]", true);
				Assert.fail("InCorrect value of data ["+dataResponse+"]");
			}
			// No credits should be deducted in db
			if(baseProdRemainingQuantityDbAfter == baseProdRemainingQuantityDbBefore) {
				// Reporter.log(", BaseProdRemainingQuantity not updated in db[Correct]", true);
			}
			else{
				Reporter.log(", BaseProdRemainingQuantity updated incorrectly in db["+baseProdRemainingQuantityDbAfter+"]", true);
				Assert.fail("BaseProdRemainingQuantity updated incorrectly in db["+baseProdRemainingQuantityDbAfter+"]");
			}
			break;
		}
		default : {
			Reporter.log(", InCorrect value of data ["+dataResponse+"]", true);
			Assert.fail("InCorrect value of data ["+dataResponse+"]");
		}
		}
	}

	// check update in 'listings_main' table
	public void verifyUpdateInlistingsMain(String subsId, String consumedId, String consumedType){
		ResultSet rs = exceuteDbQuery("SELECT listing_type_id FROM listings_main where SubscriptionId = "+subsId+" and listing_type_id = '"+consumedId+"' and "
				+ "listing_type = '"+consumedType+"' and status in ('draft','queued');","shiksha");

		int rowcount = getCountOfResultSet(rs);
		if(rowcount == 1 ) {
			// Reporter.log(", updates done correctly in 'listings_main'", true);
		}
		else{
			Reporter.log(", updates not done correctly in 'listings_main", true);
			Assert.fail(", updates not done correctly in 'listings_main");
		}
	}
}