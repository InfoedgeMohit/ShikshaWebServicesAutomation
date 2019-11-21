package courseData.info;

import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import common.Common;
import common.DatabaseCommon;

public class getCourseEligilibityExamScore extends Common {
	static String apiPath;
	String api;
	static String[] params;
	Response apiResponse = null;
	String apiResponseMsgExpected;
	static String testDataFilePath;
	static ArrayList<Integer> responseTimes = new ArrayList<Integer>();
	boolean result;
	String file = "Logs//getCourseEligibilityExamScore";
	HashMap<Object, Object> jsonMap;
	String sqlQuery = null;
	String ExpectedActualListingId = null;
	String path = "data.eligibility";
	String catvalue, keyname;
	@BeforeClass
	public void doItBeforeTest() throws FileNotFoundException {
		// apiResponseExpectedTime = 200; // in MS
		loadPropertiesFromConfig("apifacade");
		// api path
		apiPath = "apigateway/courseapi/v1/info/getCourseData";
		api = serverPath + apiPath;
		// API params in sequence followed by apiResponseMsgExpected
		params = new String[] { "courseId", "cityId", "localityId",
				"apiResponseMsgExpected"};
		// test data file
		testDataFilePath = "//src//test//resources//courseData//info//GetCourseData.xlsx";
		PrintWriter pw = new PrintWriter(file + ".txt");
		pw.close();

	}

	@DataProvider
	public static String[][] testData() throws Exception {
		// The number of times data is repeated, test will be executed the same
		// no. of
		// times
		String[][] testData = prepareTestData(params, testDataFilePath);
		return testData;
	}

	@Test(dataProvider = "testData", priority = 1)
	public void verifyGetCoursedataEligilibityExamScoreApi(String courseId, String cityId,
			String localityId, String apiResponseMsgExpected) {
		this.apiResponseMsgExpected = apiResponseMsgExpected;
		// pass api params and hit apicompareCourseId
//		courseId="294735";
		if (courseId.equals("ignoreHeader")) {
			return;
			// apiResponse = RestAssured.given().param("userId",
			// userId).param("compareCourseIds[]",compareCourseId
			// ).when().post(api).then().extract().response();
		} else if (courseId.equals("ignoreInput")) {
			apiResponse = RestAssured.given()
					.header("AUTHREQUEST", "INFOEDGE_SHIKSHA").when().get(api)
					.then().extract().response();
		} else if (!courseId.equals("ignoreInput")) {
			if (!cityId.equals("ignoreInput")) {
				if (localityId.equals("ignoreInput")) {
					apiResponse = RestAssured.given()
							.header("AUTHREQUEST", "INFOEDGE_SHIKSHA")
							.when().get(api+"?courseId="+courseId+"&cityId="+cityId).then()
							.extract().response();
				} else {
					apiResponse = RestAssured.given()
							.header("AUTHREQUEST", "INFOEDGE_SHIKSHA")
							.when().get(api+"?courseId="+courseId+"&cityId="+cityId+"&localityId="+localityId)
							.then().extract().response();
				}
			} else {
				if (localityId.equals("ignoreInput")) {
					apiResponse = RestAssured.given()
							.header("AUTHREQUEST", "INFOEDGE_SHIKSHA")
							.when().get(api+"?courseId="+courseId)
							.then().extract().response();
				} else {
					apiResponse = RestAssured.given()
							.header("AUTHREQUEST", "INFOEDGE_SHIKSHA")
							.when().get(api+"?courseId="+courseId+"&localityId="+localityId)
							.then().extract().response();
				}
			}
		}

		// fetch status code from response
		int statusCode = apiResponse.getStatusCode();
		JsonPath jsonPath = apiResponse.jsonPath();
		if (statusCode == 200) { // Positive case
			Reporter.log("Positive case", true);
			verifyPostiveCases(courseId, cityId, localityId, jsonPath);
		} else if (statusCode == 400) { // Negative case
			Reporter.log("Negative case", true);
			verifyNegativeCases(params, jsonPath, null, apiResponseMsgExpected);
		} else if (statusCode == 403) { // unauthorized request
			Reporter.log("Unauthorized request case", true);
			verifyUnauthorizedRequestCase(jsonPath);
		} else {
			Reporter.log(", InCorrect Response Code : " + statusCode, true);
			Assert.fail("InCorrect Response Code : " + statusCode);
		}

		long apiResponseActualTime = apiResponse
				.getTimeIn(TimeUnit.MILLISECONDS);
		responseTimes.add((int) apiResponseActualTime);
		if (apiResponseActualTime > apiResponseExpectedTime) {
			Reporter.log("<br> API Response time : " + apiResponseActualTime
					+ " MS is greater than expected time ("
					+ apiResponseExpectedTime + " MS)", true);
			Assert.fail("API Response time : " + apiResponseActualTime
					+ " MS is greater than expected time ("
					+ apiResponseExpectedTime + " MS)");
		} else {
			Reporter.log("<br> API Response time : " + apiResponseActualTime
					+ " MS", true);
		}
	}

	@SuppressWarnings("unchecked")
	public void verifyPostiveCases(String courseId, String cityId,
			String localityId, JsonPath jsonPath)
	{
		// verify status is success
		String statusResponse = jsonPath.getString("status");
		if (statusResponse.contentEquals("success")) {
			Reporter.log(", Correct status [success]", true);
		} else {
			Reporter.log(", InCorrect status [" + statusResponse + "]", true);
			Assert.fail("InCorrect status [" + statusResponse + "]");
		}

		// verify message is null
		String messageResponse = jsonPath.getString("message");
		if (messageResponse == null) {
			Reporter.log(", Correct message [null]", true);
		} else {
			Reporter.log(", InCorrect message [" + messageResponse + "]", true);
			Assert.fail("InCorrect message [" + messageResponse + "]");
		}
		Map<String, String> categoryList = DatabaseCommon.getEligibiltyCategoryList(courseId);
		for(Map.Entry<String, String> catentry: categoryList.entrySet()){
		 catvalue = catentry.getValue();
		try{
			//Categorywise Exam cutoff
			ResultSet rs = DatabaseCommon.getEligibilityCutOffDataforExam(courseId, catvalue);
			int resultCount = getCountOfResultSet(rs);
			if(resultCount>0){
				while(rs.next()){
				String DBexamId = String.valueOf(rs.getInt("exam_id"));
				String DBexamName =null;
				if(DBexamId.equals("0")){
					DBexamName = rs.getString("exam_name");					
				}
				else
				{
					DBexamName = DatabaseCommon.getExamname(DBexamId);
				}
				jsonMap = new HashMap<>(jsonPath.getMap(path+".exams"));
				String keyname = "exam:"+DBexamId;
				if(DBexamId.equals("0")){
					keyname = DBexamName;
				}
				String DBcategory = rs.getString("category");
				String DBunit = rs.getString("unit");
				String DBvalue = String.valueOf(rs.getInt("value"));
				String DBmaxValue = String.valueOf(rs.getInt("max_value"));
				String DBexamUrl = DatabaseCommon.getExamUrl(DBexamId);
				
				String relatedState = DatabaseCommon.getEligibilityExamRelatedState(courseId,DBexamId);
				HashMap<Object, Object> jsonCutoffDataMap =(HashMap<Object, Object>) parseJsonData(jsonMap, keyname, "cutOffData");
				if(relatedState!=null && relatedState.length()>0){
					HashMap<Object, Object> dbRelatedStateMap = new HashMap<Object, Object>();
					relatedState = relatedState.replace("related_states:", "");
					String[] dbrelatedstate = relatedState.split(",");
					for(int i=0;i<dbrelatedstate.length;i++){
					dbRelatedStateMap.put(dbrelatedstate[i], DatabaseCommon.getStateName(dbrelatedstate[i]));					
					}
					for(Map.Entry<Object, Object> entry: dbRelatedStateMap.entrySet()){
						String dbstateId = (String) entry.getKey();
						String dbstateName = (String) entry.getValue();
						String jsonStateName =(String) parseJsonData(jsonCutoffDataMap, "relatedStates", dbstateId);
						if(dbstateName.equalsIgnoreCase(jsonStateName)){
							Reporter.log("Related State Name is appear correct for courseId: "+ courseId+" examId: "+DBexamId+"StateId: "+dbstateId, true);
						}
						else{
							Reporter.log("Related State Name is not appear correct for courseId: "+ courseId+" examId: "+DBexamId+"StateId: "+dbstateId, true);
							filewriter("Related State Name is not appear correct for courseId: "+ courseId+" examId: "+DBexamId+"StateId: "+dbstateId, file);
							filewriter("Actual Result: "+jsonStateName+" Expected Result: "+dbstateName, file);						
							Assert.assertTrue(false, "Related State Name is not appear correct for courseId: "+ courseId+" examId: "+DBexamId+" StateId: "+dbstateId);
						}
					}
				}
				else{
					if(jsonCutoffDataMap!=null){
						HashMap<Object, Object> jsonrelatedStateMap = (HashMap<Object, Object>) parseJsonData(jsonCutoffDataMap, keyname, "relatedStates");
						if(jsonrelatedStateMap!=null){
							Reporter.log("RelatedState Data is not appear null so related state Failed for courseId: "+courseId+" ExamId: "+DBexamId, true);
							filewriter("RelatedState Data is not appear null so related state Failed for courseId: "+courseId+" ExamId: "+DBexamId, file);
							filewriter("Actual Result: "+jsonrelatedStateMap, file);
							Assert.assertTrue(false, "RelatedState Data is not appear null so related state Failed for courseId: "+courseId+" ExamId: "+DBexamId);
							
						}
						else{
							Reporter.log("Cutoff Data appear null so related state passed for courseId: "+courseId+" ExamId: "+DBexamId, true);
						}
					}
					else{
						Reporter.log("Cutoff Data appear null so related state passed for courseId: "+courseId+" ExamId: "+DBexamId, true);
					}
				}
				//fetch value from Jsondata
				
				
				String jsonScoreType =(String) parseJsonData(jsonMap, keyname, "scoreType");				
				if(StringUtils.equals(DBunit,jsonScoreType)){
					Reporter.log("scoreType is appear correct for courseId: "+courseId+" category: "+catvalue, true);
				}
				else{
					Reporter.log("scoreType is not appear correct for courseId: "+courseId+" category: "+catvalue, true);
					filewriter("scoreType is not appear correct for courseId: "+courseId+" category: "+catvalue, file);
					filewriter("actual Result: "+jsonScoreType+" Expected Result: "+DBunit, file);
					Assert.assertTrue(false,"scoreType is not appear correct for courseId: "+courseId+" category: "+catvalue);
				}
				
				String jsonExamName =(String) parseJsonData(jsonMap, keyname, "examName");
				if(DBexamName.equals(jsonExamName)){
					Reporter.log("examName is appear correct for courseId: "+courseId+" category: "+catvalue, true);
				}
				else{
					Reporter.log("examName is not appear correct for courseId: "+courseId+" category: "+catvalue, true);
					filewriter("examName is not appear correct for courseId: "+courseId+" category: "+catvalue, file);
					filewriter("actual Result: "+jsonExamName+" Expected Result: "+DBexamName, file);
					Assert.assertTrue(false, "examName is not appear correct for courseId: "+courseId+" category: "+catvalue);
				}
				String jsonExamId =String.valueOf((Integer) parseJsonData(jsonMap, keyname, "examId"));
				if(DBexamId.equals(jsonExamId)){
					Reporter.log("examId is appear correct for courseId: "+courseId+" category: "+catvalue, true);
				}
				else{
					Reporter.log("examId is not appear correct for courseId: "+courseId+" category: "+catvalue, true);
					filewriter("examId is not appear correct for courseId: "+courseId+" category: "+catvalue, file);
					filewriter("actual Result: "+jsonExamId+" Expected Result: "+DBexamId, file);
					Assert.assertTrue(false, "examId is not appear correct for courseId: "+courseId+" category: "+catvalue);
				}
				
				String jsonexamUrl = (String) parseJsonData(jsonMap, keyname, "examUrl");
				if(jsonexamUrl!=null&&jsonexamUrl.length()>0){
					jsonexamUrl = jsonexamUrl.substring(jsonexamUrl.indexOf(".com/")+4);
				}
				if(DBexamUrl==null){
					DBexamUrl="";
				}
				if(jsonexamUrl==null){
					jsonexamUrl="";
				}
				if(StringUtils.equals(jsonexamUrl, DBexamUrl)){
					Reporter.log("examUrl is appear correct for courseId: "+courseId+" category: "+catvalue, true);
				}
				else{
					Reporter.log("examUrl is not appear correct for courseId: "+courseId+" category: "+catvalue, true);
					filewriter("examUrl is not appear correct for courseId: "+courseId+" category: "+catvalue, file);
					filewriter("actual Result: "+jsonexamUrl+" Expected Result: "+DBexamId, file);
					Assert.assertTrue(false, "examUrl is not appear correct for courseId: "+courseId+" category: "+catvalue);
				}
			
				jsonMap = (HashMap<Object, Object>)parseJsonData(jsonMap, keyname, "categoryWiseScores");
				String jsonExamCat = (String)parseJsonData(jsonMap, catvalue, "category");
				
				if(DBcategory.equals(jsonExamCat)){
					Reporter.log("category is appear correct for courseId: "+courseId+" category: "+catvalue, true);
				}
				else{
					Reporter.log("category is not appear correct for courseId: "+courseId+" category: "+catvalue, true);
					filewriter("category is not appear correct for courseId: "+courseId+" category: "+catvalue, file);
					filewriter("actual Result: "+jsonExamCat+" Expected Result: "+DBcategory, file);
					Assert.assertTrue(false, "category is not appear correct for courseId: "+courseId+" category: "+catvalue);
				}
				
				String jsonExamScore = String.valueOf((Integer)parseJsonData(jsonMap, catvalue, "score"));
				if(DBvalue.equals(jsonExamScore)){
					Reporter.log("score is appear correct for courseId: "+courseId+" category: "+catvalue, true);
				}
				else{
					Reporter.log("score is not appear correct for courseId: "+courseId+" category: "+catvalue, true);
					filewriter("score is not appear correct for courseId: "+courseId+" category: "+catvalue, file);
					filewriter("actual Result: "+jsonExamScore+" Expected Result: "+DBvalue, file);
					Assert.assertTrue(false, "score is not appear correct for courseId: "+courseId+" category: "+catvalue);
				}
				
				String jsonExamMaxScore = String.valueOf((Integer)parseJsonData(jsonMap, catvalue, "maxScore"));
				if(DBmaxValue.equals(jsonExamMaxScore)){
					Reporter.log("maxScore is appear correct for courseId: "+courseId+" category: "+catvalue, true);
				}
				else{
					Reporter.log("maxScore is not appear correct for courseId: "+courseId+" category: "+catvalue, true);
					filewriter("maxScore is not appear correct for courseId: "+courseId+" category: "+catvalue, file);
					filewriter("actual Result: "+jsonExamMaxScore+" Expected Result: "+DBmaxValue, file);
					Assert.assertTrue(false, "maxScore is not appear correct for courseId: "+courseId+" category: "+catvalue);
				}
			}
			}
			else{
				ResultSet rsExam = DatabaseCommon.getEligibilityCourseExams(courseId);
				if(getCountOfResultSet(rsExam)>0){
					while(rsExam.next()){
						//Fetchvalue from Database
						String DBexamId = String.valueOf(rs.getInt("exam_id"));
						String DBexamName =null;
						if(DBexamId.equals("0")){
							DBexamName = rs.getString("exam_name");					
						}
						else
						{
							DBexamName = DatabaseCommon.getExamname(DBexamId);
						}
						String DBunit = rs.getString("unit");
						String DBexamUrl = DatabaseCommon.getExamUrl(DBexamId);
						
						//fetch value from Jsondata
						jsonMap = new HashMap<>(jsonPath.getMap(path+".exams"));
						String keyname = "exam:"+DBexamId;
						if(DBexamId.equals("0")){
							keyname = DBexamName;
						}
						
						String jsonScoreType =(String) parseJsonData(jsonMap, keyname, "scoreType");
						
						if(StringUtils.equals(DBunit,jsonScoreType)){
							Reporter.log("scoreType is appear correct for courseId: "+courseId+" category: "+catvalue, true);
						}
						else{
							Reporter.log("scoreType is not appear correct for courseId: "+courseId+" category: "+catvalue, true);
							filewriter("scoreType is not appear correct for courseId: "+courseId+" category: "+catvalue, file);
							filewriter("actual Result: "+jsonScoreType+" Expected Result: "+DBunit, file);
							Assert.assertTrue(false,"scoreType is not appear correct for courseId: "+courseId+" category: "+catvalue);
						}
						
						String jsonExamName =(String) parseJsonData(jsonMap, keyname, "examName");
						if(DBexamName.equals(jsonExamName)){
							Reporter.log("examName is appear correct for courseId: "+courseId+" category: "+catvalue, true);
						}
						else{
							Reporter.log("examName is not appear correct for courseId: "+courseId+" category: "+catvalue, true);
							filewriter("examName is not appear correct for courseId: "+courseId+" category: "+catvalue, file);
							filewriter("actual Result: "+jsonExamName+" Expected Result: "+DBexamName, file);
							Assert.assertTrue(false, "examName is not appear correct for courseId: "+courseId+" category: "+catvalue);
						}
						String jsonExamId =String.valueOf((Integer) parseJsonData(jsonMap, keyname, "examId"));
						if(DBexamId.equals(jsonExamId)){
							Reporter.log("examId is appear correct for courseId: "+courseId+" category: "+catvalue, true);
						}
						else{
							Reporter.log("examId is not appear correct for courseId: "+courseId+" category: "+catvalue, true);
							filewriter("examId is not appear correct for courseId: "+courseId+" category: "+catvalue, file);
							filewriter("actual Result: "+jsonExamId+" Expected Result: "+DBexamId, file);
							Assert.assertTrue(false, "examId is not appear correct for courseId: "+courseId+" category: "+catvalue);
						}
						
						String jsonexamUrl = (String) parseJsonData(jsonMap, keyname, "examUrl");
						if(jsonexamUrl!=null&&jsonexamUrl.length()>0){
							jsonexamUrl = jsonexamUrl.substring(jsonexamUrl.indexOf(".com/")+4);
						}
						if(DBexamUrl==null){
							DBexamUrl="";
						}
						if(jsonexamUrl==null){
							jsonexamUrl="";
						}
						if(StringUtils.equals(jsonexamUrl, DBexamUrl)){
							Reporter.log("examUrl is appear correct for courseId: "+courseId+" category: "+catvalue, true);
						}
						else{
							Reporter.log("examUrl is not appear correct for courseId: "+courseId+" category: "+catvalue, true);
							filewriter("examUrl is not appear correct for courseId: "+courseId+" category: "+catvalue, file);
							filewriter("actual Result: "+jsonexamUrl+" Expected Result: "+DBexamId, file);
							Assert.assertTrue(false, "examUrl is not appear correct for courseId: "+courseId+" category: "+catvalue);
						}
					}
				}
				else{
					if(jsonPath.getString(path)!=null){
						if(jsonPath.getString(path+".exams")!=null){
							Reporter.log("Eligibility Exam Cutoff is not appear correct for CourseId: "+courseId, true);
							filewriter("Eligibility Exam Cutoff is not appear correct, while db return value 0 for CourseId: "+courseId, file);
							filewriter("actual Result:"+jsonPath.getString(path+".exams"), file);
							Assert.assertTrue(false,"Eligibility Exam Cutoff is not appear correct for CourseId: "+courseId);
						}
						else{
							Reporter.log("Eligibility Exam Cutoff is appear correct for CourseId: "+courseId, true);
						}
					}
					else{
						Reporter.log("Eligibility Exam Cutoff is appear correct for CourseId: "+courseId, true);
					}
				}
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}
		try{
			ResultSet rs = DatabaseCommon.checkEligibilityExamCutoff(courseId, catvalue);
			int resultcount = getCountOfResultSet(rs);
			if(resultcount>0){
				while(rs.next()){
					
					//Fetch Data From DB
					String dbExamId = String.valueOf(rs.getInt("exam_id"));
					String dbExamYear = String.valueOf(rs.getInt("exam_year"));
					String dbExamCustomName = rs.getString("custom_exam");
					String dbRound = String.valueOf(rs.getInt("round"));
					if(dbRound.equals("-1")){
						dbRound = "0";
					}
					String dbQuota = rs.getString("quota");
					String dbCategory = rs.getString("category");
					String dbCutOffValue = String.valueOf(rs.getInt("cut_off_value"));
					String dbunit = rs.getString("cut_off_unit");
					String dbmarksOutOff = String.valueOf(rs.getInt("exam_out_of"));

					jsonMap = new HashMap<>(jsonPath.getMap(path+".exams"));
					keyname = "exam:"+dbExamId;
					if(dbExamId.equals("0")){
						keyname = dbExamCustomName;
					}
					
					//Fetch Data from Json
					HashMap<Object, Object> jsonCutoffDataMap =(HashMap<Object, Object>) parseJsonData(jsonMap, keyname, "cutOffData");
					String jsonCutoffYear =  String.valueOf((Integer)parseJsonData(jsonCutoffDataMap, null, "cutOffYear"));
					String jsonCutoffUnit = (String)parseJsonData(jsonCutoffDataMap, null, "cutOffUnit");
					String jsonRoundApplicable = String.valueOf((Boolean) parseJsonData(jsonCutoffDataMap, null, "roundsApplicable"));
					HashMap<Object, Object> jsonRoundCutoffData = (HashMap<Object, Object>) parseJsonData(jsonCutoffDataMap, "roundsCutOffData", dbRound );
					HashMap<Object, Object> jsonTempRoundMap = (HashMap<Object, Object>) parseJsonData(jsonRoundCutoffData, catvalue, dbQuota);
					String jsonScore = String.valueOf((Integer)parseJsonData(jsonTempRoundMap, null, "score"));
					String jsonCategory = (String)parseJsonData(jsonTempRoundMap, null, "category");
					String jsonMaxScore = String.valueOf((Integer)parseJsonData(jsonTempRoundMap, null, "maxScore"));
					
					if(jsonCutoffYear.equals(dbExamYear)){
						Reporter.log("Examyear is appear correct in exam Cutoff for CourseId: "+courseId+" examId: "+dbExamId+" Category: "+catvalue+" qouta: "+dbQuota, true );
					}
					else{
						Reporter.log("Examyear is not appear correct in exam Cutoff for CourseId: "+courseId+" examId: "+dbExamId+" Category: "+catvalue+" qouta: "+dbQuota, true );
						filewriter("Examyear is not appear correct in exam Cutoff for CourseId: "+courseId+" examId: "+dbExamId+" Category: "+catvalue+" qouta: "+dbQuota, file);
						filewriter("Actual result: "+jsonCutoffYear+" expected year: "+dbExamYear, file);
						Assert.assertTrue(false, "Examyear is not appear correct in exam Cutoff for CourseId: "+courseId+" examId: "+dbExamId+" Category: "+catvalue+" qouta: "+dbQuota);
					}
					
					if(jsonCutoffUnit.equals(dbunit)){
						Reporter.log("Unit is appear correct in exam Cutoff for CourseId: "+courseId+" examId: "+dbExamId+" Category: "+catvalue+" qouta: "+dbQuota, true );
					}
					else{
						Reporter.log("Unit is not appear correct in exam Cutoff for CourseId: "+courseId+" examId: "+dbExamId+" Category: "+catvalue+" qouta: "+dbQuota, true );
						filewriter("Unit is not appear correct in exam Cutoff for CourseId: "+courseId+" examId: "+dbExamId+" Category: "+catvalue+" qouta: "+dbQuota, file);
						filewriter("Actual result: "+jsonCutoffUnit+" expected year: "+dbunit, file);
						Assert.assertTrue(false, "Unit is not appear correct in exam Cutoff for CourseId: "+courseId+" examId: "+dbExamId+" Category: "+catvalue+" qouta: "+dbQuota);
					}
					
					if(jsonRoundApplicable.equals("false")){
						Reporter.log("RoundApplicable is appear correct in exam Cutoff for CourseId: "+courseId+" examId: "+dbExamId+" Category: "+catvalue+" qouta: "+dbQuota, true );
					}
					else{
						Reporter.log("RoundApplicable is not appear correct in exam Cutoff for CourseId: "+courseId+" examId: "+dbExamId+" Category: "+catvalue+" qouta: "+dbQuota, true );
						filewriter("RoundApplicable is not appear correct in exam Cutoff for CourseId: "+courseId+" examId: "+dbExamId+" Category: "+catvalue+" qouta: "+dbQuota, file);
						filewriter("Actual result: "+jsonRoundApplicable+" expected year: false", file);
						Assert.assertTrue(false, "RoundApplicable is not appear correct in exam Cutoff for CourseId: "+courseId+" examId: "+dbExamId+" Category: "+catvalue+" qouta: "+dbQuota);
					}
					
					if(jsonScore.equals(dbCutOffValue)){
						Reporter.log("Score is appear correct in exam Cutoff for CourseId: "+courseId+" examId: "+dbExamId+" Category: "+catvalue+" qouta: "+dbQuota, true );
					}
					else{
						Reporter.log("Score is not appear correct in exam Cutoff for CourseId: "+courseId+" examId: "+dbExamId+" Category: "+catvalue+" qouta: "+dbQuota, true );
						filewriter("Score is not appear correct in exam Cutoff for CourseId: "+courseId+" examId: "+dbExamId+" Category: "+catvalue+" qouta: "+dbQuota, file);
						filewriter("Actual result: "+jsonScore+" expected year: "+dbCutOffValue, file);
						Assert.assertTrue(false, "Score is not appear correct in exam Cutoff for CourseId: "+courseId+" examId: "+dbExamId+" Category: "+catvalue+" qouta: "+dbQuota);
					}
					
					if(jsonMaxScore.equals(dbmarksOutOff)){
						Reporter.log("marksoutOff is appear correct in exam Cutoff for CourseId: "+courseId+" examId: "+dbExamId+" Category: "+catvalue+" qouta: "+dbQuota, true );
					}
					else{
						Reporter.log("marksoutOff is not appear correct in exam Cutoff for CourseId: "+courseId+" examId: "+dbExamId+" Category: "+catvalue+" qouta: "+dbQuota, true );
						filewriter("marksoutOff is not appear correct in exam Cutoff for CourseId: "+courseId+" examId: "+dbExamId+" Category: "+catvalue+" qouta: "+dbQuota, file);
						filewriter("Actual result: "+jsonMaxScore+" expected year: "+dbmarksOutOff, file);
						Assert.assertTrue(false, "marksoutOff is not appear correct in exam Cutoff for CourseId: "+courseId+" examId: "+dbExamId+" Category: "+catvalue+" qouta: "+dbQuota);
					}
					
					if(jsonCategory.equals(dbQuota)){
						Reporter.log("Category is appear correct in exam Cutoff for CourseId: "+courseId+" examId: "+dbExamId+" Category: "+catvalue+" qouta: "+dbQuota, true );
					}
					else{
						Reporter.log("Category is not appear correct in exam Cutoff for CourseId: "+courseId+" examId: "+dbExamId+" Category: "+catvalue+" qouta: "+dbQuota, true );
						filewriter("Category is not appear correct in exam Cutoff for CourseId: "+courseId+" examId: "+dbExamId+" Category: "+catvalue+" qouta: "+dbQuota, file);
						filewriter("Actual result: "+jsonCategory+" expected year: "+dbCategory, file);
						Assert.assertTrue(false, "Category is not appear correct in exam Cutoff for CourseId: "+courseId+" examId: "+dbExamId+" Category: "+catvalue+" qouta: "+dbQuota);
					}
				}
			}
			
		}
		catch(Exception e){
			Reporter.log("nullpointer Exception appear correct in exam Cutoff for CourseId: "+courseId+" Category: "+catvalue, true );
			e.printStackTrace();
		}
		
		}
		try {
			ResultSet rs = DatabaseCommon.getEligibilityCourseExams(courseId);
			int resultCount = getCountOfResultSet(rs);
			if (resultCount > 0) {
				while (rs.next()) {
					String DBexamId = String.valueOf(rs.getInt("exam_id"));
					String DBexamName =null;
					if(DBexamId.equals("0")){
						DBexamName = rs.getString("exam_name");					
					}
					else
					{
						DBexamName = DatabaseCommon.getExamname(DBexamId);
					}
					String DBunit = rs.getString("unit");
					String DBexamUrl = DatabaseCommon.getExamUrl(DBexamId);
					
					jsonMap = new HashMap<>(jsonPath.getMap(path+".exams"));
					String keyname = "exam:"+DBexamId;
					if(DBexamId.equals("0")){
						keyname = DBexamName;
					}
					String jsonScoreType =(String) parseJsonData(jsonMap, keyname, "scoreType");
					
					if(StringUtils.equals(DBunit,jsonScoreType)){
						Reporter.log("scoreType is appear correct for courseId: "+courseId, true);
					}
					else{
						Reporter.log("scoreType is not appear correct for courseId: "+courseId, true);
						filewriter("scoreType is not appear correct for courseId: "+courseId+" ExamId: "+DBexamId, file);
						filewriter("actual Result: "+jsonScoreType+" Expected Result: "+DBunit, file);
						Assert.assertTrue(false,"scoreType is not appear correct for courseId: "+courseId);
					}
					
					String jsonExamName =(String) parseJsonData(jsonMap, keyname, "examName");
					if(DBexamName.equals(jsonExamName)){
						Reporter.log("examName is appear correct for courseId: "+courseId, true);
					}
					else{
						Reporter.log("examName is not appear correct for courseId: "+courseId, true);
						filewriter("examName is not appear correct for courseId: "+courseId, file);
						filewriter("actual Result: "+jsonExamName+" Expected Result: "+DBexamName, file);
						Assert.assertTrue(false, "examName is not appear correct for courseId: "+courseId);
					}
					String jsonExamId =String.valueOf((Integer) parseJsonData(jsonMap, keyname, "examId"));
					if(DBexamId.equals(jsonExamId)){
						Reporter.log("examId is appear correct for courseId: "+courseId, true);
					}
					else{
						Reporter.log("examId is not appear correct for courseId: "+courseId, true);
						filewriter("examId is not appear correct for courseId: "+courseId, file);
						filewriter("actual Result: "+jsonExamId+" Expected Result: "+DBexamId, file);
						Assert.assertTrue(false, "examId is not appear correct for courseId: "+courseId);
					}
					
					String jsonexamUrl = (String) parseJsonData(jsonMap, keyname, "examUrl");
					if(jsonexamUrl!=null&&jsonexamUrl.length()>0){
						jsonexamUrl = jsonexamUrl.substring(jsonexamUrl.indexOf(".com/")+4);
					}
					if(DBexamUrl==null){
						DBexamUrl="";
					}
					if(jsonexamUrl==null){
						jsonexamUrl="";
					}
					if(StringUtils.equals(jsonexamUrl, DBexamUrl)){
						Reporter.log("examUrl is appear correct for courseId: "+courseId, true);
					}
					else{
						Reporter.log("examUrl is not appear correct for courseId: "+courseId, true);
						filewriter("examUrl is not appear correct for courseId: "+courseId, file);
						filewriter("actual Result: "+jsonexamUrl+" Expected Result: "+DBexamUrl, file);
						Assert.assertTrue(false, "examUrl is not appear correct for courseId: "+courseId);
					}
				
				}
			}
		} catch (SQLException e) {
			
			e.printStackTrace();
			
		}
		}
	
	
	@Test(priority = 2)
	public static void respnseTimeStats_getCourseEligilibityExamScore() {
		respnseTimeStatsCommon(responseTimes,
				apiPath.substring(apiPath.lastIndexOf("/") + 1));
	}

  

}
