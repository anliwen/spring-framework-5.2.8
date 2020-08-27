/**
 * 安利文注释
 */
package org.springframework.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.lang.Nullable;

/**
 * Utility class for working with Strings that have placeholder values in them.
 * A placeholder takes the form {@code ${name}}. Using
 * {@code PropertyPlaceholderHelper} these placeholders can be substituted for
 * user-supplied values.
 * <p>
 * Values for substitution can be supplied using a {@link Properties} instance
 * or using a {@link PlaceholderResolver}.
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @since 3.0
 */
public class PropertyPlaceholderHelper {

	private static final Log logger = LogFactory.getLog(PropertyPlaceholderHelper.class);

	private static final Map<String, String> wellKnownSimplePrefixes = new HashMap<>(4);
	
	// 占位符前后缀的查找,由于大多数占位符都是{}[]()等类型的括号
	// wellKnownSimplePrefixes的括号key和value反着放原因是一般占位符括号钱都有标识符,比如$s
	// 这样放方便嵌套{{}}时的配对查找等情况
	static {
		wellKnownSimplePrefixes.put("}", "{");
		wellKnownSimplePrefixes.put("]", "[");
		wellKnownSimplePrefixes.put(")", "(");
	}
	// 前缀,例如 "${"
	private final String placeholderPrefix;
	// 后缀,例如 "}"
	private final String placeholderSuffix;
	// 前缀部分,例如 "{"
	private final String simplePrefix;
	// 分隔符,用于占位符找不到值的默认值设置
	@Nullable
	private final String valueSeparator;
	// 找不到占位符对应value值时是否忽略查找
	private final boolean ignoreUnresolvablePlaceholders;

	/**
	 * Creates a new {@code PropertyPlaceholderHelper} that uses the supplied prefix
	 * and suffix. Unresolvable placeholders are ignored.
	 * 
	 * @param placeholderPrefix the prefix that denotes the start of a placeholder
	 * @param placeholderSuffix the suffix that denotes the end of a placeholder
	 */
	public PropertyPlaceholderHelper(String placeholderPrefix, String placeholderSuffix) {
		this(placeholderPrefix, placeholderSuffix, null, true);
	}

	/**
	 * Creates a new {@code PropertyPlaceholderHelper} that uses the supplied prefix
	 * and suffix.
	 * 
	 * @param placeholderPrefix              the prefix that denotes the start of a
	 *                                       placeholder
	 * @param placeholderSuffix              the suffix that denotes the end of a
	 *                                       placeholder
	 * @param valueSeparator                 the separating character between the
	 *                                       placeholder variable and the associated
	 *                                       default value, if any
	 * @param ignoreUnresolvablePlaceholders indicates whether unresolvable
	 *                                       placeholders should be ignored
	 *                                       ({@code true}) or cause an exception
	 *                                       ({@code false})
	 */
	// 占位符解析器的构造,例如${{a}}等各种情况,所以需要预先构建各种符号,方便解析
	public PropertyPlaceholderHelper(String placeholderPrefix, String placeholderSuffix,
			@Nullable String valueSeparator, boolean ignoreUnresolvablePlaceholders) {
		// 构造占位符解析器,前后缀都能为空
		Assert.notNull(placeholderPrefix, "'placeholderPrefix' must not be null");
		Assert.notNull(placeholderSuffix, "'placeholderSuffix' must not be null");
		this.placeholderPrefix = placeholderPrefix;
		this.placeholderSuffix = placeholderSuffix;
		String simplePrefixForSuffix = wellKnownSimplePrefixes.get(this.placeholderSuffix);
		if (simplePrefixForSuffix != null && this.placeholderPrefix.endsWith(simplePrefixForSuffix)) {
			this.simplePrefix = simplePrefixForSuffix;
		} else {
			this.simplePrefix = this.placeholderPrefix;
		}
		this.valueSeparator = valueSeparator;
		this.ignoreUnresolvablePlaceholders = ignoreUnresolvablePlaceholders;
	}

	/**
	 * Replaces all placeholders of format {@code ${name}} with the corresponding
	 * property from the supplied {@link Properties}.
	 * 
	 * @param value      the value containing the placeholders to be replaced
	 * @param properties the {@code Properties} to use for replacement
	 * @return the supplied value with placeholders replaced inline
	 */
	public String replacePlaceholders(String value, final Properties properties) {
		// properties值不能为null,否则直接报错,properties为key-value健值对
		Assert.notNull(properties, "'properties' must not be null");
		return replacePlaceholders(value, properties::getProperty);
	}

	/**
	 * Replaces all placeholders of format {@code ${name}} with the value returned
	 * from the supplied {@link PlaceholderResolver}.
	 * 
	 * @param value               the value containing the placeholders to be
	 *                            replaced
	 * @param placeholderResolver the {@code PlaceholderResolver} to use for
	 *                            replacement
	 * @return the supplied value with placeholders replaced inline
	 */
	public String replacePlaceholders(String value, PlaceholderResolver placeholderResolver) {
		// value值不能为null,否则直接报错
		Assert.notNull(value, "'value' must not be null");
		return parseStringValue(value, placeholderResolver, null);
	}

	// 在解析配置文件"application.xml"与配置文件占位符${an}中使用了该方法
	// 在构造该解析器的时候,传入的参数为(根据传入的参数构造解析器):
	// 1.placeholderPrefix = "${"
	// 2.placeholderSuffix = "}"
	// 3.simplePrefix = "{"
	// 4.valueSeparator = ":"
	// 5. ignoreUnresolvablePlaceholders = false
	protected String parseStringValue(String value, PlaceholderResolver placeholderResolver,
			@Nullable Set<String> visitedPlaceholders) {
		// 如果字符串不包含"${",则直接将字符串返回,不做任何解析
		// 例如配置文件名称"application.xml"
		int startIndex = value.indexOf(this.placeholderPrefix);
		if (startIndex == -1) {
			return value;
		}
		StringBuilder result = new StringBuilder(value);
		// 第一次进入次循环,说明给定字符串包含前缀"${"
		// 之后再次进入说明存在多个占位符,一次解析
		while (startIndex != -1) {
			// 获取该字符串前缀对应的后缀索引
			int endIndex = findPlaceholderEndIndex(result, startIndex);
			// 存在指定前缀对应的后缀
			if (endIndex != -1) {
				// StringBuilder截取,包头不包尾的截取,"${an}".substring(2,4) = "an"
				String placeholder = result.substring(startIndex + this.placeholderPrefix.length(), endIndex);
				// 这里貌似没啥用,直接使用placeholder也可以
				String originalPlaceholder = placeholder;
				// Set接口的add方法,插值的时候,该值在Set中已存在返回false
				// 则抛出异常,说名非法参数异常,在属性定义中,循环的定义了占位符的引用
				// 例如:占位符使用${a},而属性定义key-value时为a=${a},这样的话就是重复添加a值,进入了死循环
				// 此Set是防止该死循环而设计
				// 此Set参数可以由外部传入,如果传的Set不为空,也要注意防止手动导致这个异常的发生
				if (visitedPlaceholders == null) {
					visitedPlaceholders = new HashSet<>(4);
				}
				if (!visitedPlaceholders.add(originalPlaceholder)) {
					throw new IllegalArgumentException(
							"Circular placeholder reference '" + originalPlaceholder + "' in property definitions");
				}
				// Recursive invocation, parsing placeholders contained in the placeholder key.
				// 递归调用,例如解析${${an}}的情况
				placeholder = parseStringValue(placeholder, placeholderResolver, visitedPlaceholders);
				// Now obtain the value for the fully resolved key...
				// 解析到占位符最后一层,进行key-value获取值,也就是获取真正的占位符实际值
				String propVal = placeholderResolver.resolvePlaceholder(placeholder);
				// 如果获取的占位符的value为空,而分隔符不为空,需要处理特殊情况的用法
				// 比如${an:hhh},首先解析占位符aa的value,如果aa的value不存在,则使用分隔符冒号后面的hhh作为默认值
				if (propVal == null && this.valueSeparator != null) {
					// 获取分隔符的索引位置
					int separatorIndex = placeholder.indexOf(this.valueSeparator);
					// 分隔符索引不是-1则说明存在分隔符
					if (separatorIndex != -1) {
						// 获取分隔符前面的key值
						String actualPlaceholder = placeholder.substring(0, separatorIndex);
						// 获取分隔符后面的默认值
						String defaultValue = placeholder.substring(separatorIndex + this.valueSeparator.length());
						// 根据传入的key解析key对应的value
						propVal = placeholderResolver.resolvePlaceholder(actualPlaceholder);
						// 如果不存在,则使用分隔符后面的默认值
						if (propVal == null) {
							propVal = defaultValue;
						}
					}
				}
				// 解析出的value值不是空
				if (propVal != null) {
					// Recursive invocation, parsing placeholders contained in the
					// previously resolved placeholder value.
					// 继续递归调用,比如占位符为${an},而key-value为an=${val},则需要递归解析
					// 上面的递归调用是占位符中包含占位符,而此递归调用是因为获取到key-value后存在占位符
					propVal = parseStringValue(propVal, placeholderResolver, visitedPlaceholders);
					// 解析后把实际value值替换之前的占位符,例如:${an} -> kobe
					result.replace(startIndex, endIndex + this.placeholderSuffix.length(), propVal);
					if (logger.isTraceEnabled()) {
						logger.trace("Resolved placeholder '" + placeholder + "'");
					}
					// x.indexOf(a,b)的含义参考JDK,也就是从字符串x的第b个索引开始向后查找,直到找到a字符串的索引,不存在则返回-1
					// 解析完一个占位符后,需要继续向后查找是否还存在占位符,有则继续while循环解析,没有则返回-1,结束解析,返回
					startIndex = result.indexOf(this.placeholderPrefix, startIndex + propVal.length());
				} else if (this.ignoreUnresolvablePlaceholders) {
					// Proceed with unprocessed value.
					// 如果解析后没有找到value值,并且设置忽略占位符,则继续向后寻找占位符进行解析
					startIndex = result.indexOf(this.placeholderPrefix, endIndex + this.placeholderSuffix.length());
				} else {
					// 如果解析出的最终key没有对应的value并且不忽略占位符(ignoreUnresolvablePlaceholders == false)
					// 则直接抛出异常
					throw new IllegalArgumentException(
							"Could not resolve placeholder '" + placeholder + "'" + " in value \"" + value + "\"");
				}
				// 这个set只是防止一个占位符的循环引用,如果解析完一个占位符后,需要及时移出集合,防止一个字符串存在多个相同占位符的情况
				// 抛循环引用异常,比如${an}--${an}
				visitedPlaceholders.remove(originalPlaceholder);
			} else {
				// 如果不存在前缀对应的后缀,比如"${{val}",则直接跳出循环,执行方法return语句,返回原字符串
				startIndex = -1;
			}
		}
		return result.toString();
	}

	// 寻找给定字符串后缀索引,如果能进入这个方法,则说明给定字符串必包含前缀
	private int findPlaceholderEndIndex(CharSequence buf, int startIndex) {
		// startIndex = 前缀"${"的位置
		// index = "${"后面第一个字符的位置,也就是获取占位符符号中字符的位置
		int index = startIndex + this.placeholderPrefix.length();
		// "{}"嵌套层数标识符
		int withinNestedPlaceholder = 0;
		// 如果前缀后面还有字符的话
		// 在while循环中,要不找到后缀,直接返回索引
		// 要不就是index++继续向后寻找
		// 按照逻辑,index不可能大于buf.length(),只是等于小于的关系
		// 一旦等于,则说明给定字符串以前缀结尾,前缀后面没有字符,则不存在后缀,直接返回-1
		// 或者是前缀后没有寻找到后缀
		while (index < buf.length()) {
			// 定位到"}"指定后缀
			if (StringUtils.substringMatch(buf, index, this.placeholderSuffix)) {
				// 如果withinNestedPlaceholder>0,则说明存在前后缀嵌套行为,发现一次嵌套,标识符减一
				if (withinNestedPlaceholder > 0) {
					withinNestedPlaceholder--;
					// 跳过嵌套后缀长度,继续向后寻找
					index = index + this.placeholderSuffix.length();
				} else {
					// 说明没有嵌套,而且定位到"}"后缀,返回后缀索引
					return index;
				}
				// 如果buf == "${{",则执行下面的逻辑
			} else if (StringUtils.substringMatch(buf, index, this.simplePrefix)) {
				// 说明花括号中进行了嵌套,该标志位+1进行标识
				withinNestedPlaceholder++;
				// 没有找到后缀,只是发现了嵌套simplePrefix前缀,index继续向后寻找(index按前缀长度++)
				index = index + this.simplePrefix.length();
			} else {
				// 说明前缀后面既不是"{",也不是"}"(该解析器传入的标识前后缀),继续向后寻找(index按字符个数++)
				index++;
			}
		}
		// 说明没有找到与前缀匹配的后缀,直接返回-1
		return -1;
	}

	/**
	 * Strategy interface used to resolve replacement values for placeholders
	 * contained in Strings.
	 */
	// 函数式接口,实现该接口,用于给定占位符实际的值,也就是根据key给出value
	@FunctionalInterface
	public interface PlaceholderResolver {

		/**
		 * Resolve the supplied placeholder name to the replacement value.
		 * 
		 * @param placeholderName the name of the placeholder to resolve
		 * @return the replacement value, or {@code null} if no replacement is to be
		 *         made
		 */
		// 可以使用jdk8的::模式实现
		@Nullable
		String resolvePlaceholder(String placeholderName);
	}

}
