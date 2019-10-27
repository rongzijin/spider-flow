package org.spiderflow.core.executor.shape;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import org.spiderflow.context.SpiderContext;
import org.spiderflow.executor.ShapeExecutor;
import org.spiderflow.model.SpiderNode;
import org.springframework.stereotype.Component;

/**
 * 等待循环结束执行器
 * 
 * @author Administrator
 *
 */
@Component
public class LoopJoinExecutor implements ShapeExecutor {

	private static final String JOIN_NODE_ID = "joinNode";
	
	public static final String VARIABLE_CONTEXT = "__variable_context";
	
	@Override
	public void execute(SpiderNode node, SpiderContext context, Map<String, Object> variables) {
	}

	@Override
	public String supportShape() {
		return "loopJoin";
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean allowExecuteNext(SpiderNode node, SpiderContext context, Map<String, Object> variables) {
		String joinNodeId = node.getStringJsonValue(JOIN_NODE_ID);
		Collection<Map<String, Object>> variableCollection = (Collection<Map<String, Object>>) variables.get(VARIABLE_CONTEXT + joinNodeId);
		variableCollection.add(variables);
		CountDownLatch countDownLatch = (CountDownLatch) variables.get(LoopExecutor.LOOP_NODE_KEY + joinNodeId);
		if (countDownLatch != null) {
			synchronized (countDownLatch) {
				countDownLatch.countDown();
				boolean isDone = countDownLatch.getCount() == 0L;
				if(isDone){
					//清空原有
					variableCollection.stream()
						.flatMap(map -> map.entrySet().stream())
						.collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.mapping(Map.Entry::getValue, Collectors.toList())))
						.forEach((k,v)->{
							String key = "@" + k;
							if(variables.containsKey(key) == false || k.startsWith("@")){
								if(key.startsWith("@@")){
									key = k;
								}
								variables.put(key, v);
							}
						});
				}

				return isDone;
			}
		} else {
			context.error("找不到等待节点：{}" + node.getStringJsonValue(JOIN_NODE_ID));
		}
		return false;
	}
}
