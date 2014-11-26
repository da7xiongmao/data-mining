package xu.main.java.Statistics;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import xu.main.java.configure.DataMiningConfigure;
import xu.main.java.util.StringUtil;
import xu.main.java.util.TextUtil;

public class CountStatistics {

	private List<String> stopWordsList = new ArrayList<String>();

	private String inputFileCharset = "UTF-8";
	/* 训练集样本单词总数 */
	private double totalWordsNum = 0.0;

	private Set<String> totalWordsSet = new HashSet<String>();
	/* 每类文本单词总数 */
	private Map<String, Integer> categoryTotalWordsMap = new HashMap<String, Integer>();
	/* 每类文本中出现的单词和出现次数map */
	private Map<String, Map<String, Integer>> categoryWordsAndCountMap = new HashMap<String, Map<String, Integer>>();
	/* 每类文本和此类文本的先验概率 */
	Map<String, Double> categoryPriorProbability = new HashMap<String, Double>();

	public CountStatistics(String charset) {
		this.inputFileCharset = charset;
		try {
			this.loadStopWords();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public Map<String, String> matchCategoryProbability(String input) {
		String words = TextUtil.replaceCnStopWords(input, stopWordsList);
		String[] wordArray = words.split("\t");

		Map<String, String> matchResult = new HashMap<String, String>();
		Set<Entry<String, Map<String, Integer>>> set = categoryWordsAndCountMap.entrySet();
		for (Iterator<Entry<String, Map<String, Integer>>> it = set.iterator(); it.hasNext();) {
			Entry<String, Map<String, Integer>> entry = it.next();
			BigDecimal matchedProbability = new BigDecimal(1.0);
			for (String word : wordArray) {
				if (!totalWordsSet.contains(word)) {
					continue;
				}
				matchedProbability = matchedProbability.multiply(computeConditionalprobability(entry.getKey(), word));
				// matchedProbability *=
				// computeConditionalprobability(entry.getKey(), word);
			}
			matchResult.put(entry.getKey(), matchedProbability.multiply(new BigDecimal(categoryPriorProbability.get(entry.getKey()))).toEngineeringString());
		}

		return matchResult;
	}

	// 计算条件概率
	public BigDecimal computeConditionalprobability(String categoryName, String input) {
		// P(tk|C) (类C下单词tk出现次数加1 )除以 (类C下单词总数 + 训练样本中不重复特征词总数)
		return new BigDecimal(StringUtil.nullToInt(categoryWordsAndCountMap.get(categoryName).get(input)) + 1).divide(
				new BigDecimal(StringUtil.nullToInt(categoryTotalWordsMap.get(categoryName)) + totalWordsNum), BigDecimal.ROUND_CEILING);
	}

	public void buildeModel(String categoryRootPath) throws IOException {
		File categoryDirFile = new File(categoryRootPath);
		// 各类别中各个词出现次数
		for (File file : categoryDirFile.listFiles()) {
			computeWordsAndCountByGivenCategory(file, file.getName());
		}
		// 各类别总词数
		computeCategoryTotalWords();
		// 总词数
		computeTotalWordsNum();
		// 先验概率
		computePriorProbability();
	}

	// 计算先验概率
	public void computePriorProbability() {
		for (Iterator<Entry<String, Integer>> it = categoryTotalWordsMap.entrySet().iterator(); it.hasNext();) {
			Entry<String, Integer> entry = it.next();
			entry.getKey();
			entry.getValue();
			categoryPriorProbability.put(entry.getKey(), entry.getValue() / totalWordsNum);
		}
	}

	public void computeTotalWordsNum() {
		totalWordsNum = (double) totalWordsSet.size();
	}

	public void computeCategoryTotalWords() {
		Set<Entry<String, Map<String, Integer>>> set = categoryWordsAndCountMap.entrySet();
		for (Iterator<Entry<String, Map<String, Integer>>> it = set.iterator(); it.hasNext();) {
			Entry<String, Map<String, Integer>> entry = it.next();
			categoryTotalWordsMap.put(entry.getKey(), entry.getValue().size());
			// 数据汇总
			totalWordsSet.addAll(entry.getValue().keySet());
		}
	}

	public void computeWordsAndCountByGivenCategory(File categoryFile, String categoryName) throws IOException {
		if (categoryFile.isFile()) {
			Map<String, Integer> wordsAndCountMap = categoryWordsAndCountMap.get(categoryName);
			if (wordsAndCountMap == null) {
				wordsAndCountMap = new HashMap<String, Integer>();
				categoryWordsAndCountMap.put(categoryName, wordsAndCountMap);
			}
			System.out.println("加载文档 ：" + categoryFile.getAbsolutePath());
			String words = TextUtil.loadFile(categoryFile, inputFileCharset);
			String[] wordArray = words.split("\t");
			// 处理词
			for (String word : wordArray) {
				if (StringUtil.isNullOrEmpty(word)) {
					continue;
				}
				wordsAndCountMap.put(word, StringUtil.nullToInt(wordsAndCountMap.get(word)) + 1);
			}
			return;
		}
		for (File file : categoryFile.listFiles()) {
			computeWordsAndCountByGivenCategory(file, categoryName);
		}
	}

	public void loadStopWords() throws IOException {
		String[] stopWords = TextUtil.loadFile(DataMiningConfigure.CN_STOP_WORDS_FILE_PATH, inputFileCharset).split("\t");
		for (String stopWord : stopWords) {
			this.stopWordsList.add(stopWord);
		}
	}

	public static void main(String[] args) throws IOException {

		CountStatistics countStatistics = new CountStatistics("gb2312");
		countStatistics.buildeModel(DataMiningConfigure.CN_PROCESSED_OUTPUT_FILE_ROOT_PATH);

		String categoryName = "";
		BigDecimal bigDecimal = new BigDecimal(1);

		String input = "这些天在弹雨横飞，政客奔驰的世界中，英国王储查尔斯的再婚事由，格外显得休闲，无妨来品味一下这桩世坛闲话。童话里有王子必有玫瑰。玫瑰只等待王子摘取。“天生丽质难自弃，一朝选在君王侧”。前些年，大众传媒欢呼查尔斯的玫瑰为戴安娜。这位生长于平民小巷的金发美女，整个人就像是用玫瑰花瓣做成的，芳香艳丽，流光溢彩，总把她身旁的王子衬得憔悴。然而现在查尔斯拉开帷幕，向世界宣布：他的玫瑰是一位57岁的老妇卡米拉！年隐而不秘的恋情，王子经历了与美女试婚生子的过程。然而，容貌与年龄的对比，非但没有令他发生转移，反而是因比较而更加吸引。与戴安娜的新婚燕尔一过，一碟子水便见了底。查尔斯继续与旧情人约会，不断疏远如花似玉的戴王妃。在丈夫的淡弃下，人前欢笑的戴安娜曾有过自杀的举动，后来便也接受婚外情，并且不止有一。后果自然是夫妇两人都在为破坏王室尊严作贡献，誰也不再克制。谁也不甘心做王室尊严的牺牲品，用虚度人生来维持虚礼。就这一点讲，查尔斯与戴安娜都是现代人，而非童话中的城堡公主与王子。王室身份主宰不了他们的命运，他们各行其是，各择所需。　　高调的戴妃在追求她自由的过程中意外车祸。查尔斯以低调延续着他的爱情长跑。那些在查尔斯的婚礼上打出怀念戴妃标语的民众有些“嫁祸”之嫌，要怀念你平时尽可怀念，何必成心让快乐的人儿不快乐？要这一对为戴安娜的幸福和生命负责，本身就是不理性的。　　再看此时的查尔斯，虽然身旁的卡米拉一副“老干妈”模样，王子却笑得甜蜜开心，以致中年风度陡增，潇洒许多，颇具绅士派头。　　对比昔日与花样王妃的照片上，查尔斯如一颗酸梅，又干又涩，虽然青春却缺点什么。公众的玫瑰却不是他心中的玫瑰，戴安娜只做成了广告上的玫瑰。　　众人眼中的老妇，却令王子幸福得心花怒放。“知我者谓我心忧，不知我者谓我何求？”王子如懂得这两句诗，一定点头纳谢。　　35年追求不懈，穿过花丛与风雨，查尔斯的品位不是用诺言和“坚贞”之类来固定的，而是用他这35年来身为王储的特殊身份、生活经历来确认和熔铸成的。为此我祝福王子。　　我不知他作为王储和国王，在政治上会如何，到那时再说。但是作为一位中年男性，一位知识分子，和一位幸福自由度少于平民的王储，查尔斯是可以让人理解和敬重的。　　婚姻破裂的现象到处都是。今天在中国，由于法律刚刚表现出宽容态度，人们很快地接受离婚，甚至开始纵容。但在婚姻破裂的背面，有不同的社会人文背景，有生活品质上升或下滑的趋势，有能令人保持尊敬和令人无奈的差异，显示出一种对生活情趣与质量的品位。　　查尔斯与戴妃的文化差距，一开始就无可弥合。一位内向的爱好哲学的具有高学历的男子，结合于一位外向的喜欢逢场作秀的幼儿园师资阅历的女子，这本身就像是热气球碰到冷玻璃。　　戴安娜有点无辜的是，王子事先有情人并首先背叛婚约。不过当一个人决定离去，那采取什么方法便不是主要的了。戴妃的劣势除了“不知情”，更有她从平民身份里带来的依附感，故更脆弱而难以承受破裂。　　据报道，戴妃的卧室是粉红色的，保持着童趣。查尔斯无法满足于粉红色，重拾旧情。随着更多的不愉快，他否定了这朵王室为他选定的玫瑰。　　外表毫无娇艳风情的卡米拉，却仿佛是一座密闭的宝库。无疑她具有精神气质上的丰富与优势，尤其对王子个人特质的理解和涵容，使其成为查尔斯最信任的女人。处于至尊地位的王子，寻求的竟然是安全感，一个心灵的港湾。　　对于生为公众人物的王子，一出生便暴露于光天化日之下，正是多么渴望一种纯属于私人的生活。　　然而说起来，在当今中国，被离弃的却是“卡米拉式”的中年女性。失意的是一些具有内涵与精神、气质的自强女子，被选择的则是那些“戴安娜式”的粉红色女性。年轻性感，对于中国男人似乎是一块奖牌。中国多的是“卡米拉”，而“查尔斯式”的男性趋少。见异思迁，见色起意，见色忘义大有人在。　　那种千锤百炼，深如大海的感情，似乎不再属于这个古老的民族。先人“曾经沧海难为水，除去巫山不是云”的诗句，也只有送给英国人查尔斯了。　　从对异性伴侣的择取而言，从他不流俗亦不从俗的爱情个性而言，查尔斯具备一位王子的品位。";
		Map<String, String> matchResult = countStatistics.matchCategoryProbability(input);
		for (Iterator<Entry<String, String>> it = matchResult.entrySet().iterator(); it.hasNext();) {
			Entry<String, String> entry = it.next();
			System.out.print(entry.getKey());
			System.out.print("\t");
			System.out.println(entry.getValue());
			BigDecimal tempDec = new BigDecimal(entry.getValue());
			if (bigDecimal.compareTo(tempDec) == -1) {
				bigDecimal = tempDec;
				categoryName = entry.getKey();
			}
		}
		System.out.println("类型：" + categoryName);
		System.out.println(" done ...");
	}
}
