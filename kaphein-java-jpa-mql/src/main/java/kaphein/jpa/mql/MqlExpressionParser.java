package kaphein.jpa.mql;

import java.util.AbstractMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import kaphein.jpa.core.JpaEntityAttributePath;
import kaphein.jpa.core.NotImplementedException;

// TODO : Remove recursive method calls.
class MqlExpressionParser implements Iterable<MqlExpressionParseResult>
{
  @Override
  public Iterator<MqlExpressionParseResult> iterator()
  {
    return new IteratorImpl(this);
  }

  public MqlExpressionParser(
    ObjectMapper objectMapper,
    String expression,
    String entityName
  )
    throws JsonMappingException, JsonProcessingException
  {
    this.objectMapper = objectMapper;

    inputExpression = ((Map<?, ?>)this.objectMapper.readValue(expression, Map.class))
      .entrySet()
      .stream()
      .filter(entry -> String.class.isInstance(entry.getKey()))
      .map(entry -> new AbstractMap.SimpleImmutableEntry<>(
        String.class.cast(entry.getKey()),
        Object.class.cast(entry.getValue())))
      .collect(Collectors.toMap(
        Map.Entry::getKey,
        Map.Entry::getValue,
        (l, r) -> r,
        LinkedHashMap::new));
    inputEntityName = entityName;

    exprCtxStack = new LinkedList<>();
    state = 0;
  }

  public MqlExpressionParser(
    ObjectMapper objectMapper,
    Map<String, Object> expression,
    String entityName
  )
  {
    this.objectMapper = objectMapper;

    inputExpression = expression;
    inputEntityName = entityName;

    exprCtxStack = new LinkedList<>();
    state = 0;
  }

  private static interface State
  {
    int ENDED = -1;

    int BEGINING = 0;

    int PROCESS_EXPRESSION = 1;

    int ENDING = 5;
  }

  private static class IteratorImpl implements Iterator<MqlExpressionParseResult>
  {
    private IteratorImpl(MqlExpressionParser parser)
    {
      this.parser = parser;
    }

    @Override
    public boolean hasNext()
    {
      return parser.state >= 0;
    }

    @Override
    public MqlExpressionParseResult next()
    {
      return parser.doParsing();
    }

    private final MqlExpressionParser parser;
  }

  private static final Logger LOGGER = LoggerFactory.getLogger(MqlExpressionParser.class);

  @SuppressWarnings("unchecked")
  private static Map<String, Object> castToExpression(Map<?, ?> m)
  {
    return (Map<String, Object>)m;
  }

  private static boolean isOperatorName(String str)
  {
    return str.length() > 1
      && str.startsWith("$")
      && !str.contains(".");
  }

  private MqlExpressionParseResult doParsing()
  {
    final var nodeRegistry = new MqlAstNodeRegistry();

    switch(state)
    {
    case State.BEGINING:
    {
      rootNode = nodeRegistry.create(MqlAstNode.Kind.ROOT, "");

      exprCtxStack.clear();

      final var rootExpr = inputExpression;
      if(!rootExpr.containsKey("$entityName") && null != inputEntityName)
      {
        rootExpr.put("$entityName", inputEntityName);
      }

      exprCtxStack.push(new MqlExpressionContext(
        MqlExpressionContext.Kind.CLAUSE,
        null,
        rootNode,
        "sqlExists",
        rootExpr));

      state = State.PROCESS_EXPRESSION;
    }
      break;
    case State.PROCESS_EXPRESSION:
      if(exprCtxStack.isEmpty())
      {
        state = State.ENDING;
      }
      else
      {
        final var exprCtx = exprCtxStack.pop();
        final var currentNode = exprCtx.getCurrentNode();

        switch(exprCtx.getKind())
        {
        case CONDITION_TERMS:
        {
          final var exprEntries = new LinkedList<Map.Entry<String, Object>>();
          for(final var exprEntry : exprCtx.getExpression().entrySet())
          {
            exprEntries.push(exprEntry);
          }

          while(!exprEntries.isEmpty())
          {
            final var exprEntry = exprEntries.pop();
            final var key = exprEntry.getKey();
            if(null == key || key.isBlank())
            {
              throw new MqlSyntaxException("A key of an expression cannot be null or blank");
            }

            final var value = exprEntry.getValue();

            final var isSpecialName = isOperatorName(key);
            final var specialName = (isSpecialName ? key.substring(1) : null);
            LOGGER.debug("specialName == {}", specialName);

            var isClause = null != specialName && !specialName.isBlank();
            if(isClause)
            {
              switch(specialName)
              {
              case "comment":
              case "and":
              case "or":
              case "nSqlExists":
              case "sqlExists":
                isClause = true;
                break;
              default:
                isClause = false;
              }
            }

            if(isClause)
            {
              exprCtxStack.push(new MqlExpressionContext(
                MqlExpressionContext.Kind.CLAUSE,
                exprCtx,
                currentNode,
                specialName,
                value));
            }
            else if((value instanceof Map<?, ?>))
            {
              final var mapValue = (Map<?, ?>)value;
              exprCtxStack.push(new MqlExpressionContext(
                MqlExpressionContext.Kind.VALUE_OPERATOR,
                exprCtx,
                currentNode,
                key,
                castToExpression(mapValue)));
            }
            else
            {
              exprCtxStack.push(new MqlExpressionContext(
                MqlExpressionContext.Kind.VALUE_OPERATOR,
                exprCtx,
                currentNode,
                key,
                MqlExpressions.of("$eq", value)));
            }
          }
          break;
        }
        case CLAUSE:
          processClause(exprCtx);
          break;
        case VALUE_OPERATOR:
          processValueOperator(exprCtx);
          break;
        default:
          throw new RuntimeException();
        }
      }
      break;
    case State.ENDING:
      state = State.ENDED;
      break;
    default:
      state = State.ENDED;
    }

    return new MqlExpressionParseResult(
      State.ENDED == state,
      rootNode);
  }

  private void processClause(MqlExpressionContext exprCtx)
  {
    final var nodeRegistry = exprCtx.getCurrentNode().getNodeRegistry();
    final var value = exprCtx.getValue();
    final var clauseName = exprCtx.getLabel();
    final var clauseNode = nodeRegistry.create(MqlAstNode.Kind.CLAUSE, clauseName);

    switch(clauseName)
    {
    case "comment":
      if(!(value instanceof String))
      {
        throw new MqlSyntaxException(String.format("The term of %s clause must be a string", clauseName));
      }

      final var commentText = (String)value;
      clauseNode.addChild(nodeRegistry.create(MqlAstNode.Kind.LITERAL, null, commentText));
      break;
    case "and":
    case "or":
      if(!(value instanceof List<?>))
      {
        throw new MqlSyntaxException(String.format("The term of %s clause must be a string", clauseName));
      }

      final var terms = (List<?>)value;
      for(final var term : terms)
      {
        if(!(term instanceof Map<?, ?>))
        {
          throw new MqlSyntaxException(String.format("The term of %s clause must be a map", clauseName));
        }
        final var mapTerm = (Map<?, ?>)term;

        final var termNode = nodeRegistry.create(
          MqlAstNode.Kind.CLAUSE,
          "and");
        clauseNode.addChild(termNode);

        exprCtxStack.push(new MqlExpressionContext(
          MqlExpressionContext.Kind.CONDITION_TERMS,
          exprCtx,
          termNode,
          null,
          castToExpression(mapTerm)));
      }
      break;
    case "sqlExists":
    case "nSqlExists":
    {
      if(!(value instanceof Map<?, ?>))
      {
        throw new MqlSyntaxException(String.format("The term of %s clause must be a map", clauseName));
      }
      final var mapTerm = (Map<?, ?>)value;

      final var entityNameTerm = mapTerm.get("$entityName");
      if(!(entityNameTerm instanceof String))
      {
        throw new MqlSyntaxException(String.format("%s of %s clause must be a string.", "$entityName", clauseName));
      }
      final var entityName = (String)entityNameTerm;
      final var entityNameNode = nodeRegistry.create(MqlAstNode.Kind.TERM, "entityName", entityName);
      clauseNode.addChild(entityNameNode);

      final var aliasTerm = mapTerm.get("$alias");
      if(!(aliasTerm instanceof String))
      {
        throw new MqlSyntaxException(String.format("%s of %s clause must be a string.", "$alias", clauseName));
      }
      final var alias = (String)aliasTerm;
      final var aliasNode = nodeRegistry.create(MqlAstNode.Kind.TERM, "alias", alias);
      clauseNode.addChild(aliasNode);

      final var queryTerm = mapTerm.get("$query");
      if(!(queryTerm instanceof Map<?, ?>))
      {
        throw new MqlSyntaxException(String.format("%s of %s clause must be an expression.", "$query", clauseName));
      }
      final var query = (Map<?, ?>)queryTerm;
      final var queryNode = nodeRegistry.create(MqlAstNode.Kind.TERM, "query");
      clauseNode.addChild(queryNode);

      final var andClauseNode = nodeRegistry.create(MqlAstNode.Kind.CLAUSE, "and");
      queryNode.addChild(andClauseNode);

      exprCtxStack.push(new MqlExpressionContext(
        MqlExpressionContext.Kind.CONDITION_TERMS,
        exprCtx,
        andClauseNode,
        null,
        castToExpression(query)));
    }
      break;
    default:
      throw new RuntimeException();
    }

    final var currentNode = exprCtx.getCurrentNode();
    currentNode.addChild(clauseNode);
  }

  private void processValueOperator(MqlExpressionContext exprCtx)
  {
    final var lhsPath = new JpaEntityAttributePath(exprCtx.getLabel());

    final var opExpr = exprCtx.getExpression();
    final var opNameSet = new HashSet<String>();
    for(final var key : opExpr.keySet())
    {
      final var isOpName = isOperatorName(key);
      final var opName = (isOpName ? key.substring(1) : null);
      opNameSet.add(opName);
    }

    if(opNameSet.contains("attrPath"))
    {
      onOperatorAttrPath(exprCtx, false, lhsPath);
    }
    else if(opNameSet.contains("isNull"))
    {
      onOperatorIsNull(exprCtx, false, lhsPath);
    }
    else if(opNameSet.contains("lte") || opNameSet.contains("gt"))
    {
      onOperatorGt(exprCtx, opNameSet.contains("lte"), lhsPath);
    }
    else if(opNameSet.contains("lt") || opNameSet.contains("gte"))
    {
      onOperatorGte(exprCtx, opNameSet.contains("lt"), lhsPath);
    }
    else if(opNameSet.contains("ne") || opNameSet.contains("eq"))
    {
      onOperatorEq(exprCtx, opNameSet.contains("ne"), lhsPath);
    }
    else if(opNameSet.contains("nregex") || opNameSet.contains("regex"))
    {
      onOperatorRegex(exprCtx, opNameSet.contains("nregex"), lhsPath);
    }
    else if(opNameSet.contains("nlike") || opNameSet.contains("like"))
    {
      onOperatorLike(exprCtx, opNameSet.contains("nlike"), lhsPath);
    }
    else if(opNameSet.contains("nin") || opNameSet.contains("in"))
    {
      onOperatorIn(exprCtx, opNameSet.contains("nin"), lhsPath);
    }
  }

  private void onOperatorAttrPath(
    MqlExpressionContext exprCtx,
    boolean negated,
    JpaEntityAttributePath lhsPath
  )
  {
    final var nodeRegistry = exprCtx.getCurrentNode().getNodeRegistry();
    final var expr = exprCtx.getExpression();

    final var opName = "attrPath";
    final var key = "$" + opName;
    final var value = expr.get(key);
    if(!(value instanceof String))
    {
      throw new MqlSyntaxException(String.format("The operand of %s clause must be a string", opName));
    }
    final var pathText = (String)value;
    final var path = new JpaEntityAttributePath(pathText);

    final var opNode = nodeRegistry.create(MqlAstNode.Kind.VALUE_OPERATOR, opName);
    opNode.addChild(nodeRegistry.create(MqlAstNode.Kind.LITERAL, null, path));

    final var currentNode = exprCtx.getCurrentNode();
    currentNode.addChild(opNode);
  }

  private void onOperatorIn(
    MqlExpressionContext exprCtx,
    boolean negated,
    JpaEntityAttributePath lhsPath
  )
  {
    final var nodeRegistry = exprCtx.getCurrentNode().getNodeRegistry();
    final var expr = exprCtx.getExpression();

    final var opName = (negated ? "nin" : "in");
    final var key = "$" + opName;
    final var value = expr.get(key);
    if(!(value instanceof List<?>))
    {
      throw new MqlSyntaxException(String.format("The operand of %s operator must be a list", opName));
    }

    final var opNode = nodeRegistry.create(MqlAstNode.Kind.VALUE_OPERATOR, opName);
    opNode.addChild(nodeRegistry.create(MqlAstNode.Kind.ATTRIBUTE_PATH, null, lhsPath));
    final var params = (List<?>)value;
    for(final var param : params)
    {
      opNode.addChild(nodeRegistry.create(MqlAstNode.Kind.LITERAL, null, param));
    }

    final var currentNode = exprCtx.getCurrentNode();
    currentNode.addChild(opNode);
  }

  private void onOperatorGt(
    MqlExpressionContext exprCtx,
    boolean negated,
    JpaEntityAttributePath lhsPath
  )
  {
    final var nodeRegistry = exprCtx.getCurrentNode().getNodeRegistry();
    final var expr = exprCtx.getExpression();

    final var opName = (negated ? "lte" : "gt");
    final var key = "$" + opName;
    final var value = expr.get(key);

    final var opNode = nodeRegistry.create(MqlAstNode.Kind.VALUE_OPERATOR, opName);
    opNode.addChild(nodeRegistry.create(MqlAstNode.Kind.ATTRIBUTE_PATH, null, lhsPath));
    if(value instanceof MqlAstNode)
    {
      final var rhsNode = (MqlAstNode)value;
      opNode.addChild(rhsNode);
    }
    else
    {
      opNode.addChild(nodeRegistry.create(MqlAstNode.Kind.LITERAL, null, value));
    }

    final var currentNode = exprCtx.getCurrentNode();
    currentNode.addChild(opNode);
  }

  private void onOperatorGte(
    MqlExpressionContext exprCtx,
    boolean negated,
    JpaEntityAttributePath lhsPath
  )
  {
    final var nodeRegistry = exprCtx.getCurrentNode().getNodeRegistry();
    final var expr = exprCtx.getExpression();

    final var opName = (negated ? "lt" : "gte");
    final var key = "$" + opName;
    final var value = expr.get(key);

    final var opNode = nodeRegistry.create(MqlAstNode.Kind.VALUE_OPERATOR, opName);
    opNode.addChild(nodeRegistry.create(MqlAstNode.Kind.ATTRIBUTE_PATH, null, lhsPath));
    if(value instanceof MqlAstNode)
    {
      final var rhsNode = (MqlAstNode)value;
      opNode.addChild(rhsNode);
    }
    else
    {
      opNode.addChild(nodeRegistry.create(MqlAstNode.Kind.LITERAL, null, value));
    }

    final var currentNode = exprCtx.getCurrentNode();
    currentNode.addChild(opNode);
  }

  private void onOperatorEq(
    MqlExpressionContext exprCtx,
    boolean negated,
    JpaEntityAttributePath lhsPath
  )
  {
    final var nodeRegistry = exprCtx.getCurrentNode().getNodeRegistry();
    final var expr = exprCtx.getExpression();

    final var opName = (negated ? "ne" : "eq");
    final var key = "$" + opName;
    final var value = expr.get(key);

    final var opNode = nodeRegistry.create(MqlAstNode.Kind.VALUE_OPERATOR, opName);
    opNode.addChild(nodeRegistry.create(MqlAstNode.Kind.ATTRIBUTE_PATH, null, lhsPath));
    if(value instanceof Map<?, ?>)
    {
      final var rhsExpr = (Map<?, ?>)value;
      final var rhsExprCtx = new MqlExpressionContext(
        MqlExpressionContext.Kind.VALUE_OPERATOR,
        exprCtx,
        opNode,
        (String)rhsExpr.keySet().stream().findFirst().orElseThrow(),
        castToExpression(rhsExpr));
      processValueOperator(rhsExprCtx);
    }
    else
    {
      opNode.addChild(nodeRegistry.create(MqlAstNode.Kind.LITERAL, null, value));
    }

    final var currentNode = exprCtx.getCurrentNode();
    currentNode.addChild(opNode);
  }

  private void onOperatorIsNull(
    MqlExpressionContext exprCtx,
    boolean negated,
    JpaEntityAttributePath lhsPath
  )
  {
    final var nodeRegistry = exprCtx.getCurrentNode().getNodeRegistry();
    final var expr = exprCtx.getExpression();

    final var opName = "isNull";
    final var key = "$" + opName;
    final var value = expr.get(key);

    final var opNode = nodeRegistry.create(MqlAstNode.Kind.VALUE_OPERATOR, opName);
    opNode.addChild(nodeRegistry.create(MqlAstNode.Kind.ATTRIBUTE_PATH, null, lhsPath));

    opNode.addChild(nodeRegistry.create(MqlAstNode.Kind.LITERAL, null, value));

    final var currentNode = exprCtx.getCurrentNode();
    currentNode.addChild(opNode);
  }

  private void onOperatorRegex(
    MqlExpressionContext exprCtx,
    boolean negated,
    JpaEntityAttributePath lhsPath
  )
  {
    throw new NotImplementedException();
  }

  private void onOperatorLike(
    MqlExpressionContext exprCtx,
    boolean negated,
    JpaEntityAttributePath lhsPath
  )
  {
    final var nodeRegistry = exprCtx.getCurrentNode().getNodeRegistry();
    final var expr = exprCtx.getExpression();

    final var caseSensitive = expr.getOrDefault("$caseSensitive", true);
    if(!(caseSensitive instanceof Boolean))
    {
      throw new MqlSyntaxException(String.format("The operand of %s clause must be a boolean", "caseSensitive"));
    }

    Character escapeValue = null;
    final var escapeExprValue = expr.get("$escape");
    if(null != escapeExprValue)
    {
      if(escapeExprValue instanceof CharSequence)
      {
        final var cs = (CharSequence)escapeExprValue;
        if(cs.length() > 1)
        {
          throw new MqlSyntaxException(String.format("The operand of %s clause must be a single character", "escape"));
        }

        escapeValue = cs.charAt(0);
      }
      else if(escapeExprValue instanceof Character)
      {
        escapeValue = (Character)escapeExprValue;
      }
      else
      {
        throw new MqlSyntaxException(String.format("The operand of %s clause must be a single character", "escape"));
      }
    }

    final var opName = (negated ? "nlike" : "like");
    final var key = "$" + opName;
    final var value = expr.get(key);
    if(!(value instanceof String))
    {
      throw new MqlSyntaxException(String.format("The operand of %s clause must be a string", opName));
    }

    final var opNode = nodeRegistry.create(MqlAstNode.Kind.VALUE_OPERATOR, opName);
    opNode.addChild(nodeRegistry.create(MqlAstNode.Kind.ATTRIBUTE_PATH, null, lhsPath));
    final var formatText = (String)value;
    opNode.addChild(nodeRegistry.create(MqlAstNode.Kind.LITERAL, null, formatText));
    opNode.addChild(nodeRegistry.create(MqlAstNode.Kind.LITERAL, null, caseSensitive));
    opNode.addChild(nodeRegistry.create(MqlAstNode.Kind.LITERAL, null, escapeValue));

    final var currentNode = exprCtx.getCurrentNode();
    currentNode.addChild(opNode);
  }

  private final ObjectMapper objectMapper;

  private final Map<String, Object> inputExpression;

  private final String inputEntityName;

  private final LinkedList<MqlExpressionContext> exprCtxStack;

  private int state;

  private MqlAstNode rootNode;
}
