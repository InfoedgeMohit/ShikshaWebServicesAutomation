package sums.info;

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

public class GetAllPseudoSubsForUser extends Common {

	static String apiPath;
	String api;
	static String[] params;
	static String[] paramType;
	Response apiResponse = null;
	String apiResponseMsgExpected;
	static String testDataFilePath;
	String userId;

	static ArrayList<Integer> responseTimes = new ArrayList<Integer>();

	@BeforeClass
	public void doItBeforeTest() {
		apiResponseExpectedTime = 1000; // in MS
		// api path
		apiPath = "sums/v1/info/getAllPseudoSubsForUser";
		api = serverPath + apiPath;
		// API params in sequence followed by apiResponseMsgExpected
		params = new String[]{"userId", "apiResponseMsgExpected"};
		// type of API params in sequence
		paramType = new String[]{"int"};
		// test data file
		testDataFilePath = "//src//test/resources//sums//info//GetAllPseudoSubsForUser.xlsx";
	}

	@DataProvider
	public static String[][] testData() throws Exception {
		// The number of times data is repeated, test will be executed the same no. of times
		String[][] testData = prepareTestData(params, testDataFilePath);
		return testData;
	}

	@Test(dataProvider = "testData", priority = 1)
	public void verifyGetAllPseudoSubsForUserApi(String userId, String apiResponseMsgExpected) {
		this.apiResponseMsgExpected = apiResponseMsgExpected;
		this.userId = userId;

		// pass api params and hit api
		if(!userId.equals("ignoreInput"))
			apiResponse = RestAssured.given().param("userId",userId).when().post(api).then().extract().response();
		else if (userId.equals("ignoreInput"))
			apiResponse = RestAssured.given().when().post(api).then().extract().response();

		// fetch status code from response
		int statusCode = apiResponse.getStatusCode();
		JsonPath jsonPath = apiResponse.jsonPath();

		if(statusCode == 200){
			// In Positive cases, verify values of status, message and mapData[from Db]
			// Reporter.log("Positive case", true);
			verifyPostiveCases(jsonPath);
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

	public void verifyPostiveCases(JsonPath jsonPath){
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

			// fetch details from Subscription, Subscription_Product_Mapping and Base_Products
			ResultSet rs = exceuteDbQuery("select s.SubscriptionId, sp.SubscriptionStartDate, sp.SubscriptionEndDate, sp.TotalBaseProdQuantity, sp.baseProdPseudoRemainingQuantity, sp.BaseProductId, bp.BaseProdCategory, bp.BaseProdSubCategory from Subscription s, Subscription_Product_Mapping sp, Base_Products bp where s.SubscriptionId = sp.SubscriptionId and sp.BaseProductId = bp.BaseProductId and s.SubscrStatus = 'ACTIVE' and sp.Status = 'ACTIVE' and sp.baseProdPseudoRemainingQuantity >=0 and sp.SubscriptionEndDate >= curdate() and sp.SubscriptionStartDate <= now() and s.ClientUserId = "+userId+" order by sp.BaseProductId, sp.SubscriptionStartDate, sp.SubscriptionEndDate;","SUMS");

			// get row count of resultSet of sql query
			int rowcount = getCountOfResultSet(rs);

			if(rowcount > 0){
				// verify message is null
				String messageResponse = jsonPath.getString("message");
				if(messageResponse == null) {
					// Reporter.log(", Correct message [null]", true);
				}
				else{
					Reporter.log(", InCorrect message ["+messageResponse+"]", true);
					Assert.fail("InCorrect message ["+messageResponse+"]");
				}

				// verify details in listData
				int i=0;
				while(rs.next() && i<rowcount){
					// Reporter.log("<br> Veriify data of listData["+i+"] >>", true);
					// check value of SubscriptionId
					int subsIdDb = rs.getInt("SubscriptionId");
					int subsIdresponse = jsonPath.getInt("listData["+i+"].SubscriptionId");
					if(subsIdresponse == subsIdDb) {
						// Reporter.log(", Correct value of SubscriptionId ["+subsIdDb+"]", true);
					}
					else{
						Reporter.log(", InCorrect value of SubscriptionId ["+subsIdDb+"]", true);
						Assert.fail("InCorrect value of SubscriptionId ["+subsIdDb+"]");
					}

					// check value of BaseProductId
					int baseProductDb = rs.getInt("BaseProductId");
					int baseProductResponse = jsonPath.getInt("listData["+i+"].BaseProductId");
					if(baseProductResponse == baseProductDb) {
						// Reporter.log(", Correct value of BaseProductId ["+baseProductDb+"]", true);
					}
					else{
						Reporter.log(", InCorrect value of BaseProductId ["+baseProductDb+"]", true);
						Assert.fail("InCorrect value of BaseProductId ["+baseProductDb+"]");
					}

					// check value of TotalBaseProdQuantity
					int totalBaseProdQuantityDb = rs.getInt("TotalBaseProdQuantity");
					int totalBaseProdQuantityResponse = jsonPath.getInt("listData["+i+"].TotalBaseProdQuantity");
					if(totalBaseProdQuantityResponse == totalBaseProdQuantityDb) {
						// Reporter.log(", Correct value of TotalBaseProdQuantity ["+totalBaseProdQuantityDb+"]", true);
					}
					else{
						Reporter.log(", InCorrect value of TotalBaseProdQuantity ["+totalBaseProdQuantityDb+"]", true);
						Assert.fail("InCorrect value of TotalBaseProdQuantity ["+totalBaseProdQuantityDb+"]");
					}

					// check value of baseProdPseudoRemainingQuantity
					int baseProdRemainingQuantityDb = rs.getInt("baseProdPseudoRemainingQuantity");
					int baseProdRemainingQuantityResponse = jsonPath.getInt("listData["+i+"].BaseProdPseudoRemainingQuantity");
					if(baseProdRemainingQuantityResponse == baseProdRemainingQuantityDb) {
						// Reporter.log(", Correct value of baseProdPseudoRemainingQuantity ["+baseProdRemainingQuantityDb+"]", true);
					}
					else{
						Reporter.log(", InCorrect value of baseProdPseudoRemainingQuantity ["+baseProdRemainingQuantityDb+"]", true);
						Assert.fail("InCorrect value of baseProdPseudoRemainingQuantity ["+baseProdRemainingQuantityDb+"]");
					}

					// check value of SubscriptionStartDate
					String subscriptionStartDateDb = rs.getString("SubscriptionStartDate");
					String subscriptionStartDateResponse = jsonPath.getString("listData["+i+"].SubscriptionStartDate");
					if(subscriptionStartDateDb != null )
						subscriptionStartDateDb = subscriptionStartDateDb.substring(0, 19);
					if(subscriptionStartDateDb == null &&  subscriptionStartDateResponse == subscriptionStartDateDb) {
				// Reporter.log(", Correct value of SubscriptionStartDate ["+subscriptionStartDateDb+"]", true);
					}
					else if(subscriptionStartDateDb != null && subscriptionStartDateResponse.equals(subscriptionStartDateDb)) {
						// Reporter.log(", Correct value of SubscriptionStartDate ["+subscriptionStartDateDb+"]", true);
					}
					else{
						Reporter.log(", InCorrect value of SubscriptionStartDate ["+subscriptionStartDateDb+"]", true);
						Assert.fail("InCorrect value of SubscriptionStartDate ["+subscriptionStartDateDb+"]");
					}

					// check value of SubscriptionEndDate
					String subscriptionEndDateDb = rs.getString("SubscriptionEndDate");
					String subscriptionEndDateResponse = jsonPath.getString("listData["+i+"].SubscriptionEndDate");
					if(subscriptionEndDateDb != null )
						subscriptionEndDateDb = subscriptionEndDateDb.substring(0, 19);
					if(subscriptionEndDateDb == null && subscriptionEndDateResponse == subscriptionEndDateDb) {
						// Reporter.log(", Correct value of SubscriptionEndDate ["+subscriptionEndDateDb+"]", true);
					}
					else if(subscriptionEndDateDb != null && subscriptionEndDateResponse.equals(subscriptionEndDateDb)) {
						// Reporter.log(", Correct value of SubscriptionEndDate ["+subscriptionEndDateDb+"]", true);
					}
					else{
						Reporter.log(", InCorrect value of SubscriptionEndDate ["+subscriptionEndDateDb+"]", true);
						Assert.fail("InCorrect value of SubscriptionEndDate ["+subscriptionEndDateDb+"]");
					}

					// check value of BaseProdCategory
					String baseProdCategoryDb = rs.getString("BaseProdCategory");
					String baseProdCategoryResponse = jsonPath.getString("listData["+i+"].BaseProdCategory");
					if(baseProdCategoryResponse.equals(baseProdCategoryDb)) {
						// Reporter.log(", Correct value of BaseProdCategory ["+baseProdCategoryDb+"]", true);
					}
					else{
						Reporter.log(", InCorrect value of BaseProdCategory ["+baseProdCategoryDb+"]", true);
						Assert.fail("InCorrect value of BaseProdCategory ["+baseProdCategoryDb+"]");
					}

					// check value of BaseProdSubCategory
					String baseProdSubCategoryDb = rs.getString("BaseProdSubCategory");
					String baseProdSubCategoryResponse = jsonPath.getString("listData["+i+"].BaseProdSubCategory");
					if(baseProdSubCategoryResponse.equals(baseProdSubCategoryDb)) {
						// Reporter.log(", Correct value of BaseProdSubCategory ["+baseProdSubCategoryDb+"]", true);
					}
					else{
						Reporter.log(", InCorrect value of BaseProdSubCategory ["+baseProdSubCategoryDb+"]", true);
						Assert.fail("InCorrect value of BaseProdSubCategory ["+baseProdSubCategoryDb+"]");
					}

					i++;
				}
			}else{
				// verify listData is blank
				String listDataResponse = jsonPath.getJsonObject("listData").toString();
				if(listDataResponse.equals("[]")) {
					// Reporter.log(", Correct listData []", true);
				}
				else{
					Reporter.log(", InCorrect listData ["+listDataResponse+"]", true);
					Assert.fail("InCorrect listData ["+listDataResponse+"]");
				}

				// verify message
				String messageResponse = jsonPath.getString("message");
				if(messageResponse.contentEquals("This user don`t have any ACTIVE pseudo subscriptions.")) {
					// Reporter.log(", Correct message [This user don`t have any ACTIVE pseudo subscriptions.]", true);
				}
				else{
					Reporter.log(", InCorrect message ["+messageResponse+"]", true);
					Assert.fail("InCorrect message ["+messageResponse+"]");
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@Test (priority = 2)
	public static void respnseTimeStats_GetAllPseudoSubsForUser(){
		respnseTimeStatsCommon(responseTimes, apiPath.substring(apiPath.lastIndexOf("/")+1));
	}
}