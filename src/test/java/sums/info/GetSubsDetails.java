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

public class GetSubsDetails extends Common {

	static String apiPath = "sums/v1/info/getSubsDetails";
	String api = serverPath + apiPath;
	static String[] params;
	static String[] paramType;
	Response apiResponse = null;
	String apiResponseMsgExpected;
	String subsId;

	static ArrayList<Integer> responseTimes = new ArrayList<Integer>();

	@BeforeClass
	public void doItBeforeTest() {
		apiResponseExpectedTime = 1000; // in MS
		// API params in sequence followed by apiResponseMsgExpected
		params = new String[]{"subsId", "apiResponseMsgExpected"};
		// type of API params in sequence
		paramType = new String[]{"int"};
	}

	@DataProvider
	public static String[][] testData() throws Exception {
		// test data file
		String testDataFilePath = "//src//test/resources//sums//info//GetSubsDetails.xlsx";
		// The number of times data is repeated, test will be executed the same no. of times
		String[][] testData = prepareTestData(params, testDataFilePath);
		return testData;
	}

	@Test(dataProvider = "testData", priority = 1)
	public void verifyGetSubsDetailsApi(String subsId, String apiResponseMsgExpected) {
		this.apiResponseMsgExpected = apiResponseMsgExpected;
		this.subsId = subsId;

		// pass api params and hit api
		if(!subsId.equals("ignoreInput"))
			apiResponse = RestAssured.given().param("subsId",subsId).when().post(api).then().extract().response();
		else if (subsId.equals("ignoreInput"))
			apiResponse = RestAssured.given().when().post(api).then().extract().response();

		// fetch status code from response
		int statusCode = apiResponse.getStatusCode();
		JsonPath jsonPath = apiResponse.jsonPath();

		if(statusCode == 200){
			// In Positive cases, verify values of status, message and mapData[from Db]
			// Reporter.log("Positive case", true);
			verifyPostiveCases(subsId, jsonPath);
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

	public void verifyPostiveCases(String subsId, JsonPath jsonPath){
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

			// fetch details from Subscription_Product_Mapping and Base_Products
			ResultSet rs = exceuteDbQuery("SELECT s.*, bp.BaseProdCategory, bp.BaseProdSubCategory, bp.BaseProdType, bp.Description FROM "
					+ "Subscription_Product_Mapping s, Base_Products bp where  s.status = 'ACTIVE' and s.BaseProductId = bp.BaseProductId and "
					+ "s.SubscriptionId = "+subsId+";","SUMS");

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

				// verify subscription details in listData
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

					// check value of BaseProdRemainingQuantity
					int baseProdRemainingQuantityDb = rs.getInt("BaseProdRemainingQuantity");
					int baseProdRemainingQuantityResponse = jsonPath.getInt("listData["+i+"].BaseProdRemainingQuantity");
					if(baseProdRemainingQuantityResponse == baseProdRemainingQuantityDb) {
						// Reporter.log(", Correct value of BaseProdRemainingQuantity ["+baseProdRemainingQuantityDb+"]", true);
					}
					else{
						Reporter.log(", InCorrect value of BaseProdRemainingQuantity ["+baseProdRemainingQuantityDb+"]", true);
						Assert.fail("InCorrect value of BaseProdRemainingQuantity ["+baseProdRemainingQuantityDb+"]");
					}

					// check value of SubscriptionStartDate
					String subscriptionStartDateDb = rs.getString("SubscriptionStartDate");
					String subscriptionStartDateResponse = jsonPath.getString("listData["+i+"].SubscriptionStartDate");
					if(subscriptionStartDateDb != null )
						subscriptionStartDateDb = subscriptionStartDateDb.substring(0, 19);
					if(subscriptionStartDateDb == null &&  subscriptionStartDateResponse == subscriptionStartDateDb) {
						// Reporter.log(", Correct value of SubscriptionStartDate ["+subscriptionStartDateDb+"]", true);
					} else if(subscriptionStartDateDb != null && subscriptionStartDateResponse.equals(subscriptionStartDateDb)) {
						// Reporter.log(", Correct value of SubscriptionStartDate ["+subscriptionStartDateDb+"]", true);
					} else{
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
					} else if(subscriptionEndDateDb != null && subscriptionEndDateResponse.equals(subscriptionEndDateDb)) {
						// Reporter.log(", Correct value of SubscriptionEndDate ["+subscriptionEndDateDb+"]", true);
					} else{
						Reporter.log(", InCorrect value of SubscriptionEndDate ["+subscriptionEndDateDb+"]", true);
						Assert.fail("InCorrect value of SubscriptionEndDate ["+subscriptionEndDateDb+"]");
					}

					// check value of SubscrLastModifyTime
					String subscrLastModifyTimeDb = rs.getString("SubscrLastModifyTime");
					String subscrLastModifyTimeResponse = jsonPath.getString("listData["+i+"].SubscrLastModifyTime");
					if(subscrLastModifyTimeDb != null )
						subscrLastModifyTimeDb = subscrLastModifyTimeDb.substring(0, 19);
					if(subscrLastModifyTimeDb == null && subscrLastModifyTimeResponse == subscrLastModifyTimeDb) {
						// Reporter.log(", Correct value of SubscrLastModifyTime ["+subscrLastModifyTimeDb+"]", true);
					}
					else if(subscrLastModifyTimeDb != null && subscrLastModifyTimeResponse.equals(subscrLastModifyTimeDb)) {
						// Reporter.log(", Correct value of SubscrLastModifyTime ["+subscrLastModifyTimeDb+"]", true);
					}
					else{
						Reporter.log(", InCorrect value of SubscrLastModifyTime ["+subscrLastModifyTimeDb+"]", true);
						Assert.fail("InCorrect value of SubscrLastModifyTime ["+subscrLastModifyTimeDb+"]");
					}

					// check value of Status
					String statusSubsDb = rs.getString("Status");
					String statusSubsResponse = jsonPath.getString("listData["+i+"].Status");
					if(statusSubsResponse.equals(statusSubsDb)) {
						// Reporter.log(", Correct value of Status ["+statusSubsDb+"]", true);
					}
					else{
						Reporter.log(", InCorrect value of Status ["+statusSubsDb+"]", true);
						Assert.fail("InCorrect value of Status ["+statusSubsDb+"]");
					}

					// check value of sumsEditingUserId
					String sumsEditingUserIdDb = rs.getString("sumsEditingUserId");
					String sumsEditingUserIdResponse = jsonPath.getString("listData["+i+"].sumsEditingUserId");
					if(sumsEditingUserIdDb == null && sumsEditingUserIdResponse == sumsEditingUserIdDb) {
						// Reporter.log(", Correct value of sumsEditingUserId ["+sumsEditingUserIdDb+"]", true);
					} else if (sumsEditingUserIdDb != null && sumsEditingUserIdResponse.equals(sumsEditingUserIdDb)) {
						// Reporter.log(", Correct value of sumsEditingUserId ["+sumsEditingUserIdDb+"]", true);
					} else{
						Reporter.log(", InCorrect value of sumsEditingUserId ["+sumsEditingUserIdDb+"]", true);
						Assert.fail("InCorrect value of sumsEditingUserId ["+sumsEditingUserIdDb+"]");
					}

					// check value of oldRemainingQuantity
					String oldRemainingQuantityDb = rs.getString("oldRemainingQuantity");
					String oldRemainingQuantityResponse = jsonPath.getString("listData["+i+"].oldRemainingQuantity");
					if(oldRemainingQuantityDb == null && oldRemainingQuantityResponse == oldRemainingQuantityDb) {
						// Reporter.log(", Correct value of oldRemainingQuantity ["+oldRemainingQuantityDb+"]", true);
					} else if(oldRemainingQuantityDb != null && oldRemainingQuantityResponse.equals(oldRemainingQuantityDb)) {
						// Reporter.log(", Correct value of oldRemainingQuantity ["+oldRemainingQuantityDb+"]", true);
					} else{
						Reporter.log(", InCorrect value of oldRemainingQuantity ["+oldRemainingQuantityDb+"]", true);
						Assert.fail("InCorrect value of oldRemainingQuantity ["+oldRemainingQuantityDb+"]");
					}

					// check value of oldSubscriptionStartDate
					String oldSubscriptionStartDateDb = rs.getString("oldSubscriptionStartDate");
					String oldSubscriptionStartDateResponse = jsonPath.getString("listData["+i+"].oldSubscriptionStartDate");
					if(oldSubscriptionStartDateDb != null )
						oldSubscriptionStartDateDb = oldSubscriptionStartDateDb.substring(0, 19);
					if(oldSubscriptionStartDateDb == null && oldSubscriptionStartDateResponse == oldSubscriptionStartDateDb) {
						// Reporter.log(", Correct value of oldSubscriptionStartDate ["+oldSubscriptionStartDateDb+"]", true);
					} else if(oldSubscriptionStartDateDb != null && oldSubscriptionStartDateResponse.equals(oldSubscriptionStartDateDb)) {
						// Reporter.log(", Correct value of oldSubscriptionStartDate ["+oldSubscriptionStartDateDb+"]", true);
					} else{
						Reporter.log(", InCorrect value of oldSubscriptionStartDate ["+oldSubscriptionStartDateDb+"]", true);
						Assert.fail("InCorrect value of oldSubscriptionStartDate ["+oldSubscriptionStartDateDb+"]");
					}

					// check value of oldSubscriptionEndDate
					String oldSubscriptionEndDateDb = rs.getString("oldSubscriptionEndDate");
					String oldSubscriptionEndDateResponse = jsonPath.getString("listData["+i+"].oldSubscriptionEndDate");
					if(oldSubscriptionEndDateDb != null )
						oldSubscriptionEndDateDb = oldSubscriptionEndDateDb.substring(0, 19);
					if(oldSubscriptionEndDateDb == null && oldSubscriptionEndDateResponse == oldSubscriptionEndDateDb) {
						// Reporter.log(", Correct value of oldSubscriptionEndDate ["+oldSubscriptionEndDateDb+"]", true);
					} else if(oldSubscriptionEndDateDb != null && oldSubscriptionEndDateResponse.equals(oldSubscriptionEndDateDb)) {
						// Reporter.log(", Correct value of oldSubscriptionEndDate ["+oldSubscriptionEndDateDb+"]", true);
					} else{
						Reporter.log(", InCorrect value of oldSubscriptionEndDate ["+oldSubscriptionEndDateDb+"]", true);
						Assert.fail("InCorrect value of oldSubscriptionEndDate ["+oldSubscriptionEndDateDb+"]");
					}

					// check value of disableComments
					String disableCommentsDb = rs.getString("disableComments");
					String disableCommentsResponse = jsonPath.getString("listData["+i+"].disableComments");
					if(disableCommentsDb == null && disableCommentsResponse == disableCommentsDb) {
						// Reporter.log(", Correct value of disableComments ["+disableCommentsDb+"]", true);
					} else if(disableCommentsDb != null && disableCommentsResponse.equals(disableCommentsDb)) {
						// Reporter.log(", Correct value of disableComments ["+disableCommentsDb+"]", true);
					} else{
						Reporter.log(", InCorrect value of disableComments ["+disableCommentsDb+"]", true);
						Assert.fail("InCorrect value of disableComments ["+disableCommentsDb+"]");
					}

					// check value of BaseProdPseudoRemainingQuantity
					int baseProdPseudoRemainingQuantityDb = rs.getInt("BaseProdPseudoRemainingQuantity");
					int baseProdPseudoRemainingQuantityResponse = jsonPath.getInt("listData["+i+"].BaseProdPseudoRemainingQuantity");
					if(baseProdPseudoRemainingQuantityResponse == baseProdPseudoRemainingQuantityDb) {
						// Reporter.log(", Correct value of BaseProdPseudoRemainingQuantity ["+baseProdPseudoRemainingQuantityDb+"]", true);
					} else{
						Reporter.log(", InCorrect value of BaseProdPseudoRemainingQuantity ["+baseProdPseudoRemainingQuantityDb+"]", true);
						Assert.fail("InCorrect value of BaseProdPseudoRemainingQuantity ["+baseProdPseudoRemainingQuantityDb+"]");
					}

					// check value of BaseProdCategory
					String baseProdCategoryDb = rs.getString("BaseProdCategory");
					String baseProdCategoryResponse = jsonPath.getString("listData["+i+"].BaseProdCategory");
					if(baseProdCategoryResponse.equals(baseProdCategoryDb)) {
						// Reporter.log(", Correct value of BaseProdCategory ["+baseProdCategoryDb+"]", true);
					} else{
						Reporter.log(", InCorrect value of BaseProdCategory ["+baseProdCategoryDb+"]", true);
						Assert.fail("InCorrect value of BaseProdCategory ["+baseProdCategoryDb+"]");
					}

					// check value of BaseProdSubCategory
					String baseProdSubCategoryDb = rs.getString("BaseProdSubCategory");
					String baseProdSubCategoryResponse = jsonPath.getString("listData["+i+"].BaseProdSubCategory");
					if(baseProdSubCategoryResponse.equals(baseProdSubCategoryDb)) {
						// Reporter.log(", Correct value of BaseProdSubCategory ["+baseProdSubCategoryDb+"]", true);
					} else{
						Reporter.log(", InCorrect value of BaseProdSubCategory ["+baseProdSubCategoryDb+"]", true);
						Assert.fail("InCorrect value of BaseProdSubCategory ["+baseProdSubCategoryDb+"]");
					}

					// check value of BaseProdType
					String baseProdTypeDb = rs.getString("BaseProdType");
					String baseProdTypeResponse = jsonPath.getString("listData["+i+"].BaseProdType");
					if(baseProdTypeResponse.equals(baseProdTypeDb)) {
						// Reporter.log(", Correct value of BaseProdType ["+baseProdTypeDb+"]", true);
					} else{
						Reporter.log(", InCorrect value of BaseProdType ["+baseProdTypeDb+"]", true);
						Assert.fail("InCorrect value of BaseProdType ["+baseProdTypeDb+"]");
					}

					// check value of Description
					String descriptionDb = rs.getString("Description");
					String descriptionResponse = jsonPath.getString("listData["+i+"].Description");
					if(descriptionResponse.equals(descriptionDb)) {
						// Reporter.log(", Correct value of Description ["+descriptionDb+"]", true);
					} else{
						Reporter.log(", InCorrect value of Description ["+descriptionDb+"]", true);
						Assert.fail("InCorrect value of Description ["+descriptionDb+"]");
					}

					i++;
				}
			}else{
				// verify listData is blank
				String listDataResponse = jsonPath.getJsonObject("listData").toString();
				if(listDataResponse.equals("[]")) {
					// Reporter.log(", Correct listData []", true);
				} else{
					Reporter.log(", InCorrect listData ["+listDataResponse+"]", true);
					Assert.fail("InCorrect listData ["+listDataResponse+"]");
				}

				// verify message
				String messageResponse = jsonPath.getString("message");
				if(messageResponse.contentEquals("Subscription is INACTIVE.")) {
					// Reporter.log(", Correct message [Subscription is INACTIVE.]", true);
				} else{
					Reporter.log(", InCorrect message ["+messageResponse+"]", true);
					Assert.fail("InCorrect message ["+messageResponse+"]");
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@Test (priority = 2)
	public static void respnseTimeStats_GetSubsDetails(){
		respnseTimeStatsCommon(responseTimes, apiPath.substring(apiPath.lastIndexOf("/")+1));
	}
}