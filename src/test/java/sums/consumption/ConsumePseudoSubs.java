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

public class ConsumePseudoSubs extends Common {

	static String apiPath;
	String api;
	static String[] params;
	static String[] paramType;
	Response apiResponse = null;
	String apiResponseMsgExpected;
	static String testDataFilePath;
	String subsId;
	String consumeQuant;
	int baseProdPseudoRemainingQuantityDbBefore;

	static ArrayList<Integer> responseTimes = new ArrayList<Integer>();

	@BeforeClass
	public void doItBeforeTest() {
		apiResponseExpectedTime = 1000; // in MS
		// api path
		apiPath = "sums/v1/consume/consumePseudoSubs";
		api = serverPath + apiPath;
		// API params in sequence followed by apiResponseMsgExpected
		params = new String[]{"subsId","consumeQuant","clientId","sumsId","consumedId","consumedType","remainQuant","baseProductId","startDate","endDate","apiResponseMsgExpected"};
		// type of API params in sequence
		paramType = new String[]{"Integer","Integer","Integer","Integer","Integer","String","Integer","Integer","Date","Date"};
		// test data file
		testDataFilePath = "//src//test/resources//sums//consumption//ConsumePseudoSubs.xlsx";
	}

	@DataProvider
	public static String[][] testData() throws Exception {
		// The number of times data is repeated, test will be executed the same no. of times
		String[][] testData = prepareTestData(params, testDataFilePath);
		return testData;
	}

	@Test(dataProvider = "testData", priority = 1)
	public void verifyConsumePseudoSubsApi(String subsId, String consumeQuant, String clientId, String sumsId, String consumedId, 
			String consumedType, String remainQuant, String baseProductId, String startDate, String endDate, String apiResponseMsgExpected) {
		this.apiResponseMsgExpected = apiResponseMsgExpected;
		this.subsId = subsId;
		this.consumeQuant = consumeQuant;

		// pass api params and hit api
		if(subsId.equals("ignoreInput") && consumeQuant.equals("ignoreInput") && clientId.equals("ignoreInput") && sumsId.equals("ignoreInput") && consumedId.equals("ignoreInput") && consumedType.equals("ignoreInput"))
			apiResponse = RestAssured.given().when().post(api).then().extract().response();
		else if(!subsId.equals("ignoreInput") && consumeQuant.equals("ignoreInput") && clientId.equals("ignoreInput") && sumsId.equals("ignoreInput") && consumedId.equals("ignoreInput") && consumedType.equals("ignoreInput"))
			apiResponse = RestAssured.given().
			param("subsId",subsId).
			when().post(api).then().extract().response();
		else if(subsId.equals("ignoreInput") && !consumeQuant.equals("ignoreInput") && clientId.equals("ignoreInput") && sumsId.equals("ignoreInput") && consumedId.equals("ignoreInput") && consumedType.equals("ignoreInput"))
			apiResponse = RestAssured.given().
			param("consumeQuant",consumeQuant).
			when().post(api).then().extract().response();
		else if(!subsId.equals("ignoreInput") && !consumeQuant.equals("ignoreInput") && clientId.equals("ignoreInput") && sumsId.equals("ignoreInput") && consumedId.equals("ignoreInput") && consumedType.equals("ignoreInput"))
			apiResponse = RestAssured.given().
			param("subsId",subsId).
			param("consumeQuant",consumeQuant).
			when().post(api).then().extract().response();
		else if(!subsId.equals("ignoreInput") && !consumeQuant.equals("ignoreInput") && !clientId.equals("ignoreInput") && sumsId.equals("ignoreInput") && consumedId.equals("ignoreInput") && consumedType.equals("ignoreInput"))
			apiResponse = RestAssured.given().
			param("subsId",subsId).
			param("consumeQuant",consumeQuant).
			param("clientId",clientId).
			when().post(api).then().extract().response();
		else if(!subsId.equals("ignoreInput") && !consumeQuant.equals("ignoreInput") && !clientId.equals("ignoreInput") && !sumsId.equals("ignoreInput") && consumedId.equals("ignoreInput") && consumedType.equals("ignoreInput"))
			apiResponse = RestAssured.given().
			param("subsId",subsId).
			param("consumeQuant",consumeQuant).
			param("clientId",clientId).
			param("sumsId",sumsId).
			when().post(api).then().extract().response();
		else if(!subsId.equals("ignoreInput") && !consumeQuant.equals("ignoreInput") && !clientId.equals("ignoreInput") && !sumsId.equals("ignoreInput") && !consumedId.equals("ignoreInput") && consumedType.equals("ignoreInput"))
			apiResponse = RestAssured.given().
			param("subsId",subsId).
			param("consumeQuant",consumeQuant).
			param("clientId",clientId).
			param("sumsId",sumsId).
			param("consumedId",consumedId).
			when().post(api).then().extract().response();
		else if(!subsId.equals("ignoreInput") && !consumeQuant.equals("ignoreInput") && !clientId.equals("ignoreInput") && !sumsId.equals("ignoreInput") && !consumedId.equals("ignoreInput") && !consumedType.equals("ignoreInput")){
			// fetch baseProdPseudoRemainingQuantityDbBefore from db before hitting api
			if(apiResponseMsgExpected.equalsIgnoreCase("NA"))
				baseProdPseudoRemainingQuantityDbBefore = fetchBaseProdPseudoRemainingQuantityFromDb();
			apiResponse = RestAssured.given().
					param("subsId",subsId).
					param("consumeQuant",consumeQuant).
					param("clientId",clientId).
					param("sumsId",sumsId).
					param("consumedId",consumedId).
					param("consumedType",consumedType).
					param("remainQuant",remainQuant).
					param("baseProductId",baseProductId).
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
			ResultSet rs = exceuteDbQuery("SELECT BaseProdPseudoRemainingQuantity, BaseProductId FROM Subscription_Product_Mapping where status = 'ACTIVE' AND "
					+ "DATE(subscriptionEndDate) >= CURDATE() AND DATE(subscriptionStartDate) <= CURDATE() and SubscriptionId = "+subsId+" ;","SUMS");

			// get row count of resultSet of sql query
			int rowcount = getCountOfResultSet(rs);
			int baseProdPseudoRemainingQuantityDbAfter = baseProdPseudoRemainingQuantityDbBefore; // assigning as current value
			if(rowcount > 0){
				rs.next();
				baseProdPseudoRemainingQuantityDbAfter = rs.getInt("BaseProdPseudoRemainingQuantity");
				int consumeQuantInput = Integer.parseInt(consumeQuant);
				int remainQuantInput = (remainQuant == "") ? 100 : Integer.parseInt(remainQuant);

				// verify value of {result:null, logId:null, ERROR:1} [case2] if baseProdPseudoRemainingQuantityDbBefore <= 0 or 
				// baseProdPseudoRemainingQuantityDbBefore < consumeQuantInput or remainQuantInput < consumeQuantInput 
				if(baseProdPseudoRemainingQuantityDbBefore <= 0 || baseProdPseudoRemainingQuantityDbBefore < consumeQuantInput || remainQuantInput < consumeQuantInput)
					verifyDataInResponse(2, jsonPath, baseProdPseudoRemainingQuantityDbAfter, consumeQuantInput);
				else { // verify value of {result:1, logId:null, ERROR:null} [case1] in this case
					verifyDataInResponse(1, jsonPath, baseProdPseudoRemainingQuantityDbAfter, consumeQuantInput);

					// check update done correctly in 'listings_main' table if consumption type in {course, institute, university, scholarship, notification}
					String[] consumedTypeArr = {"course","institute","university","scholarship","notification"};
					if(Arrays.asList(consumedTypeArr).contains(consumedType)) {
						String baseProductIdDb = rs.getString("BaseProductId"); // baseProductId provided in input is not considered here
						verifyUpdateInlistingsMain(subsId, consumedId, consumedType, baseProductIdDb);
					}
				}
			}else
				// verify value of {result:null, logId:null, ERROR:1} [case2] if not valid subsId (so no results from db)
				verifyDataInResponse(2, jsonPath, baseProdPseudoRemainingQuantityDbAfter, 0);
		}catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@Test (priority = 2)
	public static void respnseTimeStats_ConsumePseudoSubs(){
		respnseTimeStatsCommon(responseTimes, apiPath.substring(apiPath.lastIndexOf("/")+1));
	}

	// fetch BaseProdRemainingQuantity from db before hitting api
	public int fetchBaseProdPseudoRemainingQuantityFromDb(){
		int baseProdPseudoRemainingQuantityDb = 0;

		ResultSet rs = exceuteDbQuery("SELECT BaseProdPseudoRemainingQuantity FROM Subscription_Product_Mapping where status = 'ACTIVE' AND "
				+ "DATE(subscriptionEndDate) >= CURDATE() AND DATE(subscriptionStartDate) <= CURDATE() and SubscriptionId = "+subsId+" ;","SUMS");

		int rowcount = getCountOfResultSet(rs);
		if(rowcount > 0){
			try {
				rs.next();
				baseProdPseudoRemainingQuantityDb = rs.getInt("BaseProdPseudoRemainingQuantity");
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return baseProdPseudoRemainingQuantityDb;
	}

	// verify different combo of 'data' in api response
	public void verifyDataInResponse(int caseType, JsonPath jsonPath, int baseProdPseudoRemainingQuantityDbAfter, int consumeQuantInput){
		// possible cases of caseType: 
		// 1 : result:1, logId:null, ERROR:null [success case] 
		// 2 : result:null, logId:null, ERROR:1 [Error case]
		String dataResponse = jsonPath.getString("data");
		String resultDataResponse = jsonPath.getString("data.result");
		String logIdDataResponse = jsonPath.getString("data.logId");
		String errorDataResponse = jsonPath.getString("data.ERROR");

		switch(caseType){
		// caseType = 1 {result:1, logId:null, ERROR:1 [success case]} 
		case 1 : {
			if(resultDataResponse.equalsIgnoreCase("1") && logIdDataResponse == null && errorDataResponse == null) {
				// Reporter.logog(", Correct value of data [result:1, logId:null, ERROR:null]", true);
			}
			else{
				Reporter.log(", InCorrect value of data ["+dataResponse+"]", true);
				Assert.fail("InCorrect value of data ["+dataResponse+"]");
			}
			// Correct credits should be deducted in db
			if(baseProdPseudoRemainingQuantityDbAfter == baseProdPseudoRemainingQuantityDbBefore - consumeQuantInput) {
				// Reporter.log(", BaseProdPseudoRemainingQuantity["+baseProdPseudoRemainingQuantityDbAfter+"] updated correctly in db", true);
			}
			else{
				Reporter.log(", BaseProdPseudoRemainingQuantity["+baseProdPseudoRemainingQuantityDbAfter+"] not updated correctly in db", true);
				Assert.fail("BaseProdPseudoRemainingQuantity["+baseProdPseudoRemainingQuantityDbAfter+"] not updated correctly in db");
			}
			break;
		}
		// caseType = 2 {result:null, logId:null, ERROR:1 [Error case]}
		case 2 : {
			if(resultDataResponse == null && logIdDataResponse == null && errorDataResponse.equalsIgnoreCase("1")) {
				// Reporter.log(", Correct value of data [result:null, logId:null, ERROR:1]", true);
			}
			else{
				Reporter.log(", InCorrect value of data ["+dataResponse+"]", true);
				Assert.fail("InCorrect value of data ["+dataResponse+"]");
			}
			// No credits should be deducted in db
			if(baseProdPseudoRemainingQuantityDbAfter == baseProdPseudoRemainingQuantityDbBefore) {
				// Reporter.log(", BaseProdPseudoRemainingQuantity not updated in db[Correct]", true);
			}
			else{
				Reporter.log(", BaseProdPseudoRemainingQuantity updated incorrectly in db["+baseProdPseudoRemainingQuantityDbAfter+"]", true);
				Assert.fail("BaseProdPseudoRemainingQuantity updated incorrectly in db["+baseProdPseudoRemainingQuantityDbAfter+"]");
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
	public void verifyUpdateInlistingsMain(String subsId, String consumedId, String consumedType, String baseProdId){
		ResultSet rs = exceuteDbQuery("SELECT listing_type_id FROM listings_main where SubscriptionId = "+subsId+" and listing_type_id = '"+consumedId+"' and "
				+ "listing_type = '"+consumedType+"' and pack_type = '"+baseProdId+"' and status in ('draft','queued');","shiksha");

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
