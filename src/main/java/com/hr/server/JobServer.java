package com.hr.server;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.hr.utils.AESUtils;
import com.hr.utils.ChineseSpliter;
import com.hr.utils.StringUtils;

import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;

/***
 * 全自动投递简历
 * 
 * @author HuRui
 *
 */
public class JobServer {
	private static Map<String, String> cookies = new HashMap<>();// 登录后保存的Cookies
	private static final int TIME_OUT = 3000;// 访问超时时间

	class CompanyEntity {
		private String companyName;
		private boolean satisfy;

		public String getCompanyName() {
			return companyName;
		}

		public void setCompanyName(String companyName) {
			this.companyName = companyName;
		}

		public boolean getSatisfy() {
			return satisfy;
		}

		public void setSatisfy(boolean satisfy) {
			this.satisfy = satisfy;
		}

	}

	// 返回信息
	private static class Msg {
		private static final String SYS_ERROR = "系统错误";
		private static final String SUCCESS = "投递成功";
		private static final String REPEAT = "重复投递";
		private static final String NO_DEFAULT = "未选择投递简历";
	}

	// 用户信息
	private static class UserInfo {
		private static final String USERNAME = "707773854@qq.com";
		private static final String PASSWORD = "";// AES加密
	}

	// 公司性质枚举类
	enum CompanyType {
		/** 外资（欧美） */
		FOREIGN_INVESTMENT_EUROPE("01"),
		/** 外资（非欧美） */
		FOREIGN_INVESTMENT_NON_EUROPE("02"),
		/** 合资 */
		JOINT_VENTURE("03"),
		/** 国企 */
		STATE_OWNED_ENTERPRISE("04"),
		/** 民营公司 */
		PRIVATE_COMPANY("05"),
		/** 外企代表处 */
		FOREIGN_REPRESENTATIVE_OFFICE("06"),
		/** 政府机关 */
		GOVERNMENT_AGENCIES("07"),
		/** 事业单位 */
		INSTITUTIONS("08"),
		/** 非营利机构 */
		NONPROFIT_ORGANIZATION("09"),
		/** 上市公司 */
		LISTED_COMPANY("10"),
		/** 创业公司 */
		START_UP_COMPANIES("11");

		private String code;

		private CompanyType(String code) {
			this.code = code;
		}

		public String getCode() {
			return code;
		}

		public void setCode(String code) {
			this.code = code;
		}
	}

	// 工作城市枚举类
	enum jobArea {
		北京("010000"), 上海("020000"), 广州("030200"), 深圳("040000"), 武汉("180200"),西安("200200"),杭州("080200"),南京("070200"),成都("090200"),
		重庆("060000"),东莞("030800"),大连("230300"),苏州("070300"),昆明("250200"),长沙("190200"),合肥("150200"),宁波("080300"),郑州("170200"),
		天津("050000"),青岛("120300"),济南("120200"),哈尔滨("220200"),长春("240200"),福州("110200");
		
		/**
		 * 这里用枚举有点不太通用且维护麻烦，因时间原因懒得改了，以后有时间再改成关键字自动匹配id
		 * */ 
		
		private String areaId;

		private jobArea(String areaId) {
			this.areaId = areaId;
		}

		public String getAreaId() {
			return areaId;
		}

		public void setAreaId(String areaId) {
			this.areaId = areaId;
		}
	}

	// 搜索关键字 数组形式
	private static final String[] SEARCH_KEY_WORD = { "JAVAWEB", "JAVA工程师", "JAVA高级工程师", "高级JAVA工程师", "JAVA软件工程师",
			"JAVA高级软件工程师", "高级JAVA软件工程师", "JAVA开发工程师", "JAVA高级开发工程师", "高级JAVA开发工程师", "JAVA研发工程师", "JAVA高级研发工程师",
			"高级JAVA研发工程师", "JAVA" };
	// 需屏蔽的公司 不用全称
	private static final String[] IGNORE_COMPANY = { "软通动力", "中软国际", "网来云商", "烽火普天", "木仓科技", "亚鸿世纪", "智驾科技", "亚鸿世纪",
			"九派", "中安消", "智慧易视" };
	// 搜索的公司类型 不填写默认所有
	private static final CompanyType[] SEARCH_COMPANY_TYPE = {};
	// 需过滤的上班地址 只需写关键字
	private static final String[] IGNORE_WORK_LOCATION = { "未来科技城", "藏龙岛", "创业街", "总部空间", "汤逊湖" };
	// 精准上班地址 不写默认所有地址
	private static final String[] SEARCH_ACCURATE_ADDRESS = { "光谷E城", "光谷软件园", "玉树临风" };
	// 必须包含的福利关键字，不填写默认不屏蔽
	private static final String[] SEARCH_WELFARE_KEYWORD = {};
	// 月薪范围 格式 8000-12000
	private static final String PROVIDES_ALARY = "10000-15000";
	// 工作城市 不写默认北京
	private static final jobArea JOB_AREA = jobArea.武汉;

	// Header头
	private static void setHeader(Connection conn, Method paramMethod) {
		conn.header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
		conn.header("Accept-Encoding", "gzip, deflate, sdch, br");
		conn.header("Accept-Language", "zh,zh-CN;q=0.8,en;q=0.6,en-US;q=0.4,ru;q=0.2");
		conn.header("Connection", "keep-alive");
		conn.header("Host", "search.51job.com");
		conn.header("Upgrade-Insecure-Requests", "1");
		conn.header("User-Agent",
				"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/56.0.2924.87 Safari/537.36");
		conn.ignoreContentType(true);
		conn.method(paramMethod);
		conn.timeout(TIME_OUT);
		conn.cookies(cookies);
	}

	/***
	 * 登录
	 * 
	 * @param user
	 * @param pwd
	 * @return
	 */
	private boolean login(String user, String pwd) {
		boolean isLogin = false;
		cookies.clear();

		Map<String, String> map = new HashMap<>();
		map.put("lang", "c");
		map.put("action", "save");
		map.put("from_domain", "i");
		map.put("loginname", user);
		map.put("password", pwd);
		map.put("verifycode", "");
		map.put("isread", "on");
		if (StringUtils.isNullOrEmpty(pwd)) {
			return false;
		}
		Connection conn = Jsoup.connect("https://login.51job.com/login.php");
		setHeader(conn, Method.POST);
		conn.data(map);
		Response response = null;
		try {
			response = conn.execute();
			cookies = response.cookies();
			Document doc = response.parse();
			isLogin = doc != null && doc.title().contains("Message Center") ? true : false;
			System.out.println(isLogin ? "登录成功！" : "登录失败！");
			if (!isLogin || !selectDefault()) {
				isLogin = false;
				cookies.clear();
				cookies = null;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return isLogin;
	}

	/**
	 * 薪资格式化为Url参数
	 * 
	 * @return
	 */
	private String getProvidesAlaryUrl() {
		String ret = new String();
		if (!StringUtils.isNullOrEmpty(PROVIDES_ALARY) && PROVIDES_ALARY.indexOf("-") != 0) {
			String[] arr = PROVIDES_ALARY.split("-");
			int start = get(Integer.valueOf(arr[0]));
			int end = get(Integer.valueOf(arr[1]));
			if (start < end) {
				int tmp = start;
				start = end;
				end = tmp;
			}
			for (int i = end; i <= start; i++) {
				ret += "0" + i;
				if (i != start) {
					ret += "%2C";
				}
			}
		}
		return ret;
	}

	/***
	 * 通过薪资区间获取所需ID
	 * 
	 * @param s
	 * @return
	 */
	private int get(int s) {
		if (s <= 2000) return 1;
		if (s <= 3000) return 2;
		if (s <= 4500) return 3;
		if (s <= 6000) return 4;
		if (s <= 8000) return 5;
		if (s <= 10000) return 6;
		if (s <= 15000) return 7;
		if (s <= 20000) return 8;
		if (s <= 30000) return 9;
		if (s <= 40000) return 10;
		if (s <= 50000) return 11;
		if (s > 50000) return 12;
		return 0;
	}

	/**
	 * 抓取单个关键字List列表
	 * 
	 * @param kw
	 *            关键字
	 * @param list
	 * @param page
	 *            页码
	 * @return
	 */
	private List<String> getListPage(String kw, List<String> list, int... page) {

		int indexPage = 0;
		if (page == null || page.length == 0 || page.length > 1) {
			indexPage = 1;
		} else {
			indexPage = page[0];
		}
		try {
			String keyWord = URLEncoder.encode(kw, "UTF-8");
			String areaId = StringUtils.isNullOrEmpty(JOB_AREA) ? jobArea.北京.areaId : JOB_AREA.areaId;

			String url = StringUtils.isNullOrEmpty(PROVIDES_ALARY)
					? "http://search.51job.com/jobsearch/search_result.php?fromJs=1&jobarea=" + areaId
							+ "%2C00&keyword=" + keyWord
							+ "&keywordtype=2&lang=c&stype=2&postchannel=0000&fromType=1&confirmdate=9&curr_page="
							+ indexPage
					: "http://search.51job.com/jobsearch/search_result.php?fromJs=1&jobarea=" + areaId
							+ "&district=000000&funtype=0000&industrytype=00&issuedate=9&providesalary="
							+ getProvidesAlaryUrl() + "&keyword=" + keyWord + "&keywordtype=2&curr_page=" + indexPage
							+ "&lang=c&stype=1&postchannel=0000&workyear=99&degreefrom=99&jobterm=99&companysize=99&lonlat=0%2C0&radius=-1&ord_field=0&list_type=0&fromType=14&dibiaoid=0&confirmdate=9";

			url += "&cotype=";

			if (SEARCH_COMPANY_TYPE.length > 0) {
				for (int i = 0; i < SEARCH_COMPANY_TYPE.length; i++) {
					url += SEARCH_COMPANY_TYPE[i].getCode();
					if (i != SEARCH_COMPANY_TYPE.length - 1) {
						url += ",";
					}
				}
			} else {
				url += "99";
			}

			Connection conn = Jsoup.connect(url);

			setHeader(conn, Method.GET);

			Document doc = conn.execute().parse();
			Elements e = doc.getElementsByClass("el").select("p span a");// 获取连接

			for (Element element : e) {
				if (element.attr("title") != null && element.attr("title").toLowerCase().contains("java")) {
					list.add(element.attr("abs:href"));
				}
			}

			Elements e1 = doc.getElementsByClass("p_in").select("ul li");
			Elements e2 = e1.get(e1.size() - 1).select("a");

			if (e2 != null && e2.size() > 0) {
				getListPage(kw, list, indexPage + 1);// 递归翻页，可能会出现不可预知的错误（如递归
														// 次数/层次 太多/深
														// 造成的栈溢出），因时间问题暂不优化
				/**
				 * 最佳解决方案： 定义 stop 和 indexPage 全局变量，stop默认为true，indexPage默认0
				 * 根据页面 结构/关键字 判断是否有下一页，如果有下一页，则stop=false，indexPage++
				 * 外部定义一个方法，调用本方法，利用 for(;;){...} or while(true){...}
				 * 循环至stop=false为止
				 * 
				 * 注 1：因为多线程的情况下indexPage++不可靠，固此处可用原子类，具体使用方式不懂的可以百度，当然，
				 * 也可以用同步关键字synchronized（性能偏低）
				 * 2：因需翻页，List集合需定义为全局变量（也可定义为引用传递），但要注意多线程所带来的资源不安全问题
				 */
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return list;
	}

	/**
	 * 抓取所有详情页Url
	 * 
	 * @param page
	 * @return
	 */
	private List<String> getList(final int... page) {
		final List<String> list = new ArrayList<>();
		ExecutorService exe = Executors.newFixedThreadPool(100);
		for (final String kw : SEARCH_KEY_WORD) {
			exe.execute(new Runnable() {
				@Override
				public void run() {
					list.addAll(getListPage(kw, new ArrayList<String>(), page));
				}
			});
		}
		exe.shutdown();
		while (true) {
			if (exe.isTerminated()) {
				break;
			}
			try {
				TimeUnit.MILLISECONDS.sleep(100L);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return list;
	}

	/***
	 * 抓取单个详情
	 * 
	 * @param url
	 */
	private CompanyEntity getDetails(String url) {
		CompanyEntity entity = new CompanyEntity();
		try {
			Connection conn = Jsoup.connect(url);

			setHeader(conn, Method.GET);

			Document doc = conn.execute().parse();

			Elements e = doc.getElementsByClass("tCompany_main");
			if (e != null && e.size() > 0) {// 判断上班地址是否空

				boolean company = true;

				// 公司过滤
				for (String str : IGNORE_COMPANY) {
					if (e.text().contains(str)) {
						company = false;
						break;
					}
				}

				// 地址过滤
				boolean loc = true;
				for (String str : IGNORE_WORK_LOCATION) {
					if (e.text().contains(str)) {
						loc = false;
						break;
					}
				}

				// 福利关键字匹配
				boolean welfare = true;
				for (String str : SEARCH_WELFARE_KEYWORD) {
					if (!e.text().contains(str)) {
						welfare = false;
						break;
					}
				}

				// 地址精准匹配
				boolean acc_add = false;
				String address = null;
				Element e1 = doc.getElementsByClass("bmsg").get(1);
				if (e1 != null) {
					address = e1.select("p").html().replaceAll("<span (.*)>(.*)</span>", "").replaceAll("<[^>]+>", "");
				}

				if (!StringUtils.isNullOrEmpty(address)) {

					if (SEARCH_ACCURATE_ADDRESS.length > 0000000000000) {
						for (String str : SEARCH_ACCURATE_ADDRESS) {
							String[] arr = ChineseSpliter.splitArr(str.toLowerCase());// 分词
							for (int i = 0; i < arr.length; i++) {
								if (!address.contains(arr[i].toLowerCase())) {// 匹配单个词
									break;
								}
								if (arr.length == (i + 1)) {
									acc_add = true;
									break;
								}
							}
						}
					} else {
						acc_add = true;
					}
				}
				entity.setCompanyName(doc.getElementsByClass("cname").select("a").text());
				if (loc && welfare && company && acc_add) {// 判断上班地址跟福利
					entity.setSatisfy(true);
				}
			}
		} catch (Exception e) {
		}
		return entity;
	}

	public void start() throws InterruptedException {
		if (!login(UserInfo.USERNAME, AESUtils.encrypt(UserInfo.PASSWORD))) {
			return;
		}
		List<String> list = getList();
		ExecutorService exe = Executors.newFixedThreadPool(10);
		for (final String url : list) {
			exe.execute(new Runnable() {
				@Override
				public void run() {
					CompanyEntity entity = getDetails(url);
					if (entity.getSatisfy()) {
						System.out.println(delivery(url) + "：" + entity.getCompanyName());
						System.out.println("\t" + url);
					}
					// else {
					// System.err.println("不满足条件" + "：" +
					// entity.getCompanyName());
					// System.err.println("\t" + url);
					// }
				}
			});
		}
		for (;;) {
			if (exe.isTerminated()) {
				System.out.println("简历投递完毕！");
				break;
			}
			TimeUnit.MILLISECONDS.sleep(100L);
		}
	}

	/***
	 * 投递简历
	 * 
	 * @param jobUrl
	 *            招聘页面Url
	 * @return
	 */
	private String delivery(String jobUrl) {
		try {
			Connection conn = Jsoup.connect("http://my.51job.com/my/delivery/delivery.php");

			if (StringUtils.isNullOrEmpty(jobUrl)) {
				return Msg.SYS_ERROR;
			}

			Pattern pattern = Pattern.compile("http://jobs\\.51job\\.com/.*/([0-9]*).html");
			Matcher matcher = pattern.matcher(jobUrl);
			if (matcher.find()) {

				String jobId = matcher.group(1);

				if (StringUtils.isNullOrEmpty(jobId)) {
					return Msg.SYS_ERROR;
				}

				Map<String, String> map = new HashMap<>();
				map.put("rand", Math.random() + "");
				map.put("jsoncallback", "jsonp1488422089620");
				map.put("_", "1488422274362");
				map.put("jobid", "(" + jobId + ":0)");
				map.put("prd", "search.51job.com");
				map.put("prp", "01");
				map.put("cd", "jobs.51job.com");
				map.put("cp", "01");
				map.put("resumeid", "");
				map.put("cvlan", "");
				map.put("coverid", "");
				map.put("qpostset", "");
				map.put("elementname", "hidJobID");
				map.put("deliverytype", "1");
				map.put("deliverydomain", "http://my.51job.com/");
				map.put("language", "c");
				map.put("imgpath", "http://img02.51jobcdn.com/");

				setHeader(conn, Method.GET);

				conn.data(map);

				String text = conn.execute().parse().text();

				if (text.contains("投递成功")) {
					return Msg.SUCCESS;
				} else if (text.contains("申请过")) {
					return Msg.REPEAT;
				} else if (text.contains("请选择投递")) {
					return Msg.NO_DEFAULT;
				}

			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return Msg.SYS_ERROR;
	}

	/***
	 * 设置快速投递
	 * 
	 * @return
	 */
	private static boolean selectDefault() {
		boolean select = false;
		String url = "http://my.51job.com/cv/CResume/CV_CQpostsetting.php?jsoncallback=jQuery18308273298813490104_1492136464699&r=0.8921065548537528&isEnglish=0&_=1492136469938";
		try {
			Connection conn = Jsoup.connect(url);
			setHeader(conn, Method.GET);
			Document doc = conn.execute().parse();
			String html = doc.html().replaceAll("\\\\&quot;", "");
			Pattern pattern = Pattern.compile("id=\"rsmid\" value=\"(([1-9]\\d*\\.?\\d*)|(0\\.\\d*[1-9]))\"");
			Matcher matcher = pattern.matcher(html);
			if (matcher.find()) {
				String rsmid = matcher.group(1);
				String baseUrl = "http://my.51job.com/cv/CResume/CV_CQpostsetting.php?jsoncallback=jQuery18308273298813490104_1492136464699&isEnglish=0&r=0.8921065548537528&Read=1&stop_rsmid=&isAlert=1&isEnglish=0&status=1&rsmid="
						+ rsmid + "&Cn_" + rsmid + "=1&En_" + rsmid + "=0&CBase_" + rsmid + "=2&EBase_" + rsmid
						+ "=1&Ccp_" + rsmid + "=100&Ecp_" + rsmid + "=0&IsCn=1&IsEn=0&coverid=&_=1492136469938";
				Connection c = Jsoup.connect(baseUrl);
				setHeader(c, Method.GET);
				select = c.execute().parse().text().contains("成功");
				System.out.println(select ? "快速投递设置成功！" : "快速投递设置失败！");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return select;
	}

	public static void main(String[] args) {
		try {
			new JobServer().start();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
