package kaphein.jpa.mql;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.helpers.NOPLogger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.EntityType;
import kaphein.jpa.core.AssertArg;
import kaphein.jpa.core.JpaEntityAttributePath;
import kaphein.jpa.core.MetamodelUtils;
import kaphein.jpa.core.Sort;
import kaphein.jpa.core.Sort.NullOrder;
import kaphein.jpa.core.StringUtils;

public class MqlQueryBuilder<E>
{
  private static final String NULL_LITERAL = "NULL";

  public MqlQueryBuilder()
  {
    logger(null);
  }

  public MqlQueryBuilder(MqlQueryBuilder<E> src)
  {
    AssertArg.isNotNull(src, "src");

    logger = src.logger;
    em = src.em;
    entityType = src.entityType;
    entityJavaType = src.entityJavaType;
    entityAlias = src.entityAlias;
    objectMapper = src.objectMapper;
    mapFilter = src.mapFilter;
    filterJson = src.filterJson;
  }

  public MqlQueryBuilder<E> logger(Logger logger)
  {
    this.logger = Optional
      .ofNullable(logger)
      .orElse(NOPLogger.NOP_LOGGER);

    return this;
  }

  public MqlQueryBuilder<E> entityManager(EntityManager entityManager)
  {
    em = entityManager;

    return this;
  }

  public MqlQueryBuilder<E> entityType(EntityType<? extends E> entityType)
  {
    this.entityType = entityType;

    return this;
  }

  public MqlQueryBuilder<E> entityJavaType(Class<? extends E> entityJavaType)
  {
    this.entityJavaType = entityJavaType;

    return this;
  }

  public MqlQueryBuilder<E> objectMapper(ObjectMapper objectMapper)
  {
    this.objectMapper = objectMapper;

    return this;
  }

  public MqlQueryBuilder<E> filter(String filter)
  {
    filterJson = filter;

    return this;
  }

  public MqlQueryBuilder<E> filter(Map<String, Object> filter)
  {
    mapFilter = filter;

    return this;
  }

  public MqlQueryBuilder<E> orderByTerms(List<Sort.Order> orderByTerms)
  {
    this.orderByTerms = Collections.unmodifiableList(orderByTerms);

    return this;
  }

  public MqlQueryBuilder<E> limit(Integer limit)
  {
    this.limit = limit;

    return this;
  }

  public MqlQueryBuilder<E> offset(Integer offset)
  {
    this.offset = offset;

    return this;
  }

  public MqlQueryBuilder<E> offset(Long offset)
  {
    if(offset >= Integer.MAX_VALUE)
    {
      throw new IllegalArgumentException("JPA does not support offset greater than " + Integer.MAX_VALUE);
    }

    return offset(offset.intValue());
  }

  public MqlQueryBuildResult<E> build()
  {
    ensureParameters();

    return buildQueries();
  }

  private void ensureParameters()
  {
    if(null == em)
    {
      throw new IllegalArgumentException("entityManager is not set.");
    }

    ensureEntityType();

    if(null == objectMapper)
    {
      throw new IllegalArgumentException("objectMapper is not set.");
    }

    ensureMapFilter();

    if(null == entityAlias)
    {
      entityAlias = (String)mapFilter.get("$alias");
      if(null == entityAlias)
      {
        throw new IllegalArgumentException("$alias is missing.");
      }
    }

    if(null == orderByTerms)
    {
      orderByTerms = Collections.emptyList();
    }

    if(null == limit)
    {
      limit = Integer.MAX_VALUE;
    }

    if(null == offset)
    {
      offset = 0;
    }
  }

  private void ensureEntityType()
  {
    if(null == entityType)
    {
      if(null == entityJavaType)
      {
        throw new IllegalArgumentException("Either entityType or entityJavaType must be set.");
      }

      entityType = MetamodelUtils.findEntityTypeByJavaTypeOrThrow(em.getMetamodel(), entityJavaType);
    }
  }

  private void ensureMapFilter()
  {
    if(null == mapFilter)
    {
      if(StringUtils.isBlank(filterJson))
      {
        throw new IllegalArgumentException("filter is not set.");
      }
      else
      {
        try
        {
          mapFilter = ((Map<?, ?>)objectMapper
            .readValue(
              filterJson,
              Map.class))
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
        }
        catch(final JsonProcessingException jpe)
        {
          throw new MqlException(jpe.getMessage());
        }
      }
    }
  }

  private MqlQueryBuildResult<E> buildQueries()
  {
    final var parser = new MqlExpressionParser(
      objectMapper,
      mapFilter,
      entityType.getName());
    final var parseResult = StreamSupport
      .stream(
        Spliterators.spliteratorUnknownSize(parser.iterator(), Spliterator.ORDERED),
        false)
      .filter(MqlExpressionParseResult::isCompleted)
      .findFirst()
      .orElseThrow();

    final var queryBuilderCtxt = new MqlQueryBuilderContext(
      em.getMetamodel(),
      parseResult.getRootNode());

    final var subQueryContextMap = new HashMap<String, MqlSubQueryContext>();
    final var subQueryContextQueue = new LinkedList<MqlSubQueryContext>();
    final var rootNode = queryBuilderCtxt.getRootNode();
    final var nodePostIter = rootNode.postOrderIterator();
    while(nodePostIter.hasNext())
    {
      final var node = nodePostIter.next();
      logger.debug(
        "MqlAstNode == {} {} {} {}",
        node.getKind(),
        node.getLabel(),
        node.getChildCount(),
        node.getValue());

      switch(node.getKind())
      {
      case CLAUSE:
        switch(node.getLabel())
        {
        case "nJpqlExists":
        case "jpqlExists":
        {
          final var subQueryContext = new MqlSubQueryContext(
            queryBuilderCtxt,
            node);
          subQueryContextMap.put(
            node.getId(),
            subQueryContext);
          subQueryContextQueue.offer(subQueryContext);
        }
          break;
        default:
          // Does nothing.
        }
        break;
      default:
        // Does nothing.
      }
    }
    for(final var subQueryContext : subQueryContextQueue)
    {
      final var subQueryRootNode = subQueryContext.getSubQueryRootNode();

      MqlAstNode parentSubQueryRootNode = null;
      for(
        var currentNode = subQueryRootNode.getParent(); (null == parentSubQueryRootNode
          && null != currentNode); currentNode = currentNode.getParent()
      )
      {
        if(
          MqlAstNode.Kind.CLAUSE.equals(currentNode.getKind())
            && ("jpqlExists".equals(currentNode.getLabel())
              || ("nJpqlExists".equals(currentNode.getLabel())))
        )
        {
          parentSubQueryRootNode = currentNode;
        }
      }

      if(null != parentSubQueryRootNode)
      {
        final var parentSubQueryRootNodeId = parentSubQueryRootNode.getId();
        final var parentSubQueryContext = subQueryContextMap.get(parentSubQueryRootNodeId);
        if(null != parentSubQueryContext)
        {
          parentSubQueryContext.addChild(subQueryContext);
        }
      }
    }

    while(!subQueryContextQueue.isEmpty())
    {
      final var subQueryContext = subQueryContextQueue.poll();
      final var paramMap = subQueryContext.getQueryBuilderContext().getMqlParameterMap();
      final var subQueryRootNode = subQueryContext.getSubQueryRootNode();

      final var entityNameNode = subQueryContext.getEntityNameNode();
      final var entityType = subQueryContext.getEntityType();
      final var aliasNode = subQueryContext.getAliasNode();
      final var alias = subQueryContext.getAlias();
      final var queryNode = subQueryContext.getQueryNode();
      final var whereClauseRootNode = queryNode.getChildNodeAt(0);

      final var whereClauseNodeQueue = new LinkedList<MqlAstNode>();
      final var whereClauseNodeIter = whereClauseRootNode.postOrderIterator();
      while(whereClauseNodeIter.hasNext())
      {
        whereClauseNodeQueue.offer(whereClauseNodeIter.next());
      }

      while(!whereClauseNodeQueue.isEmpty())
      {
        final var node = whereClauseNodeQueue.poll();

        switch(node.getKind())
        {
        case CLAUSE:
          switch(node.getLabel())
          {
          case "comment":
          {
            final var parentNode = node.getParent();
            if(null != parentNode)
            {
              parentNode.removeChild(node);
            }
          }
            break;
          case "nJpqlExists":
          case "jpqlExists":
            // Does nothing.
            break;
          case "and":
          case "or":
          {
            final var termJpqlExprs = new ArrayList<String>();
            final var n = node.getChildCount();
            for(var i = 0; i < n; ++i)
            {
              final var termNode = node.getChildNodeAt(i);
              final var termJpqlExpr = termNode.getJpqlExpression();
              if(null == termJpqlExpr || termJpqlExpr.isBlank())
              {
                throw new MqlSyntaxException("");
              }

              termJpqlExprs.add(termJpqlExpr);
            }

            final var conjName = node.getLabel().toUpperCase();

            var jpqlExpr = "";
            switch(termJpqlExprs.size())
            {
            case 0:
              // Does nothing.
              break;
            case 1:
              jpqlExpr = termJpqlExprs.get(0);
              break;
            default:
              jpqlExpr = String.join(String.format(" %s ", conjName), termJpqlExprs);
            }
            if(!jpqlExpr.isBlank())
            {
              final var parent = node.getParent();
              final var isRootCondition = null != parent
                && MqlAstNode.Kind.TERM.equals(parent.getKind())
                && "jpqlWhere".equals(parent.getLabel());
              if(!isRootCondition)
              {
                jpqlExpr = String.format("(%s)", jpqlExpr);
              }

              node.setJpqlExpression(jpqlExpr);
            }

            for(var i = n; i > 0;)
            {
              --i;
              node.removeChild(node.getChildNodeAt(i));
            }
          }
            break;
          default:
            throw new MqlSyntaxException("");
          }
          break;
        case VALUE_OPERATOR:
          switch(node.getLabel())
          {
          case "attrPath":
          {
            final var paramNode = node.getChildNodeAt(0);
            if(!MqlAstNode.Kind.LITERAL.equals(paramNode.getKind()))
            {
              throw new MqlSyntaxException("The operand of attrPath operator must be a literal");
            }

            final var attrPath = (JpaEntityAttributePath)paramNode.getValue();
            // Verify the path.
            subQueryContext.findJavaTypeOf(attrPath);

            node.setKind(MqlAstNode.Kind.ATTRIBUTE_PATH);
            node.setLabel(null);
            node.setValue(attrPath);
            node.removeChild(paramNode);
          }
            break;
          case "nin":
          case "in":
          {
            final var lhsNode = node.getChildNodeAt(0);
            final var operandNodes = new LinkedList<MqlAstNode>();

            Class<?> lhsJavaType = null;
            var lhsStr = "";
            switch(lhsNode.getKind())
            {
            case ATTRIBUTE_PATH:
            {
              final var lhsAttrPath = (JpaEntityAttributePath)lhsNode.getValue();
              lhsJavaType = subQueryContext.findJavaTypeOf(lhsAttrPath);
              logger.debug("javaType({}) == {}", lhsAttrPath, lhsJavaType.getName());
              lhsStr += lhsAttrPath.toString();
            }
              break;
            default:
              throw new MqlSyntaxException("");
            }

            var rhsStr = "";
            for(var i = 1; i < node.getChildCount(); ++i)
            {
              final var operandNode = node.getChildNodeAt(i);

              switch(operandNode.getKind())
              {
              case LITERAL:
                // Does nothing.
                break;
              default:
                throw new MqlSyntaxException("");
              }

              operandNodes.add(operandNode);
            }
            final var operandValues = operandNodes
              .stream()
              .map(MqlAstNode::getValue)
              .toList();

            final var jacksonJavaType = objectMapper
              .getTypeFactory()
              .constructCollectionLikeType(
                List.class,
                lhsJavaType);
            final var deserializedOperands = objectMapper.convertValue(
              operandValues,
              jacksonJavaType);

            final var paramName = paramMap.issueParameterName(deserializedOperands);
            rhsStr = String.format(":%s", paramName);

            final var opStr = (node.getLabel().startsWith("n") ? "NOT IN" : "IN");

            final var jpqlExpr = String.format("%s %s %s", lhsStr, opStr, rhsStr);
            node.setJpqlExpression(jpqlExpr);

            for(final var operandNode : operandNodes)
            {
              node.removeChild(operandNode);
            }
            node.removeChild(lhsNode);
          }
            break;
          case "isNull":
          {
            final var lhsNode = node.getChildNodeAt(0);
            final var rhsNode = node.getChildNodeAt(1);

            var lhsStr = "";
            switch(lhsNode.getKind())
            {
            case ATTRIBUTE_PATH:
              lhsStr += ((JpaEntityAttributePath)lhsNode.getValue()).toString();
              break;
            default:
              throw new MqlSyntaxException("");
            }

            var opStr = "";
            switch(rhsNode.getKind())
            {
            case LITERAL:
            {
              final var value = rhsNode.getValue();
              if(null == value)
              {
                throw new MqlSyntaxException("");
              }

              opStr = (Boolean.parseBoolean(value.toString()) ? "IS" : "IS NOT");
            }
              break;
            default:
              throw new MqlSyntaxException("");
            }

            final var jpqlExpr = String.format("%s %s %s", lhsStr, opStr, NULL_LITERAL);
            node.setJpqlExpression(jpqlExpr);

            node.removeChild(rhsNode);
            node.removeChild(lhsNode);
          }
            break;
          case "lte":
          case "gt":
          case "lt":
          case "gte":
          {
            final var lhsNode = node.getChildNodeAt(0);
            final var rhsNode = node.getChildNodeAt(1);

            Class<?> lhsJavaType = null;
            var lhsStr = "";
            switch(lhsNode.getKind())
            {
            case ATTRIBUTE_PATH:
            {
              final var lhsAttrPath = (JpaEntityAttributePath)lhsNode.getValue();
              lhsJavaType = subQueryContext.findJavaTypeOf(lhsAttrPath);
              logger.debug("javaType({}) == {}", lhsAttrPath, lhsJavaType.getName());
              lhsStr += lhsAttrPath.toString();
            }
              break;
            default:
              throw new MqlSyntaxException("");
            }

            var rhsStr = "";
            switch(rhsNode.getKind())
            {
            case LITERAL:
            {
              final var value = rhsNode.getValue();
              if(null == value)
              {
                throw new MqlSyntaxException("");
              }

              final var rhsConvertedValue = objectMapper.convertValue(value, lhsJavaType);
              final var paramName = paramMap.issueParameterName(rhsConvertedValue);
              rhsStr = String.format(":%s", paramName);
            }
              break;
            case ATTRIBUTE_PATH:
              rhsStr += ((JpaEntityAttributePath)rhsNode.getValue()).toString();
              break;
            default:
              throw new MqlSyntaxException("");
            }

            var opStr = "";
            switch(node.getLabel())
            {
            case "lte":
              opStr = "<=";
              break;
            case "gt":
              opStr = ">";
              break;
            case "lt":
              opStr = "<";
              break;
            case "gte":
              opStr = ">=";
              break;
            default:
              throw new MqlSyntaxException("");
            }

            final var jpqlExpr = String.format("%s %s %s", lhsStr, opStr, rhsStr);
            node.setJpqlExpression(jpqlExpr);

            node.removeChild(rhsNode);
            node.removeChild(lhsNode);
          }
            break;
          case "ne":
          case "eq":
          {
            final var lhsNode = node.getChildNodeAt(0);
            final var rhsNode = node.getChildNodeAt(1);

            Class<?> lhsJavaType = null;
            var lhsStr = "";
            switch(lhsNode.getKind())
            {
            case ATTRIBUTE_PATH:
            {
              final var lhsAttrPath = (JpaEntityAttributePath)lhsNode.getValue();
              lhsJavaType = subQueryContext.findJavaTypeOf(lhsAttrPath);
              logger.debug("javaType({}) == {}", lhsAttrPath, lhsJavaType.getName());
              lhsStr += lhsAttrPath.toString();
            }
              break;
            default:
              throw new MqlSyntaxException("");
            }

            var rhsStr = "";
            switch(rhsNode.getKind())
            {
            case LITERAL:
            {
              final var value = rhsNode.getValue();
              if(null == value)
              {
                rhsStr = NULL_LITERAL;
              }
              else
              {
                final var rhsConvertedValue = objectMapper.convertValue(value, lhsJavaType);
                final var paramName = paramMap.issueParameterName(rhsConvertedValue);
                rhsStr = String.format(":%s", paramName);
              }
            }
              break;
            case ATTRIBUTE_PATH:
              rhsStr += ((JpaEntityAttributePath)rhsNode.getValue()).toString();
              break;
            default:
              throw new MqlSyntaxException("");
            }

            var opStr = "";
            switch(node.getLabel())
            {
            case "ne":
              if(NULL_LITERAL.equals(rhsStr))
              {
                opStr = "IS NOT";
              }
              else
              {
                opStr = "<>";
              }
              break;
            case "eq":
              if(NULL_LITERAL.equals(rhsStr))
              {
                opStr = "IS";
              }
              else
              {
                opStr = "=";
              }
              break;
            default:
              throw new MqlSyntaxException("");
            }

            final var jpqlExpr = String.format("%s %s %s", lhsStr, opStr, rhsStr);
            node.setJpqlExpression(jpqlExpr);

            node.removeChild(rhsNode);
            node.removeChild(lhsNode);
          }
            break;
          case "regex":
          case "nregex":
            throw new MqlSyntaxException("Not implemented");
          // break;
          case "like":
          case "nlike":
          {
            final var lhsNode = node.getChildNodeAt(0);
            final var rhsNode = node.getChildNodeAt(1);
            final var caseSensitiveNode = node.getChildNodeAt(2);
            final var escapeNode = node.getChildNodeAt(3);

            var lhsStr = "";
            switch(lhsNode.getKind())
            {
            case ATTRIBUTE_PATH:
              lhsStr += ((JpaEntityAttributePath)lhsNode.getValue()).toString();
              break;
            default:
              throw new MqlSyntaxException("");
            }

            var rhsStr = "";
            switch(rhsNode.getKind())
            {
            case LITERAL:
            {
              final var value = rhsNode.getValue();
              if(null == value)
              {
                rhsStr = NULL_LITERAL;
              }
              else
              {
                final var paramName = paramMap.issueParameterName(value);
                rhsStr = String.format(":%s", paramName);
              }
            }
              break;
            case ATTRIBUTE_PATH:
              rhsStr += ((JpaEntityAttributePath)rhsNode.getValue()).toString();
              break;
            default:
              throw new MqlSyntaxException("");
            }

            if(!Boolean.parseBoolean(caseSensitiveNode.getValue().toString()))
            {
              lhsStr = String.format("LOWER(%s)", lhsStr);
              rhsStr = String.format("LOWER(%s)", rhsStr);
            }

            final var negated = node.getLabel().startsWith("n");
            final var opStr = (negated ? "NOT LIKE" : "LIKE");

            var escStr = "";
            final var escapeValue = (Character)escapeNode.getValue();
            if(null != escapeValue)
            {
              final var paramName = paramMap.issueParameterName(escapeValue);
              escStr = String.format("ESCAPE :%s", paramName);
            }

            final var jpqlExpr = String.format("%s %s %s %s", lhsStr, opStr, rhsStr, escStr);
            node.setJpqlExpression(jpqlExpr);

            node.removeChild(escapeNode);
            node.removeChild(caseSensitiveNode);
            node.removeChild(rhsNode);
            node.removeChild(lhsNode);
          }
            break;
          default:
            throw new MqlSyntaxException("");
          }
          break;
        default:
          // Does nothing.
        }
      }

      final var whereClauseExpr = whereClauseRootNode.getJpqlExpression();

      queryNode.setJpqlExpression(String.format(
        "FROM %s %s %s",
        entityType.getName(),
        alias,
        ((null == whereClauseExpr || whereClauseExpr.isBlank())
          ? ""
          : String.format("WHERE %s", whereClauseExpr))));

      queryNode.removeChild(whereClauseRootNode);

      final var negated = subQueryRootNode.getLabel().startsWith("n");

      final var parentNode = subQueryRootNode.getParent();
      if(null == parentNode)
      {
        throw new MqlSyntaxException("");
      }

      switch(parentNode.getKind())
      {
      case ROOT:
      {
        if(negated)
        {
          throw new MqlSyntaxException("");
        }

        subQueryRootNode.setJpqlExpression(queryNode.getJpqlExpression());
      }
        break;
      default:
      {
        var finalJpqlExpr = String.format("EXISTS (SELECT %s %s)", alias, queryNode.getJpqlExpression());
        if(negated)
        {
          finalJpqlExpr = String.format("NOT %s", finalJpqlExpr);
        }

        subQueryRootNode.setJpqlExpression(finalJpqlExpr);
      }
      }

      subQueryRootNode.removeChild(queryNode);
      subQueryRootNode.removeChild(aliasNode);
      subQueryRootNode.removeChild(entityNameNode);
    }

    final var paramMap = queryBuilderCtxt.getMqlParameterMap();
    final var queryNode = rootNode.getChildNodeAt(0);
    final var jpqlFromClause = queryNode.getJpqlExpression();
    logger.debug("jpqlFromClause == {}", jpqlFromClause);

    final var jpqlOrderByClause = buildOrderByClause();
    logger.debug("jpqlOrderByClause == {}", jpqlOrderByClause);

    final var itemQuery = em
      .createQuery(
        String.format(
          "SELECT %s %s%s",
          entityAlias,
          jpqlFromClause,
          (StringUtils.isNotBlank(jpqlOrderByClause) ? " " + jpqlOrderByClause : "")),
        entityType.getJavaType())
      .setMaxResults(limit)
      .setFirstResult(offset);
    final var countQuery = em.createQuery(
      String.format("SELECT COUNT(*) %s", jpqlFromClause),
      Long.class);
    for(final var paramEntry : paramMap.entrySet())
    {
      final var paramName = paramEntry.getKey();
      final var paramValue = paramEntry.getValue();

      itemQuery.setParameter(paramName, paramValue);
      countQuery.setParameter(paramName, paramValue);
    }
    rootNode.removeChild(queryNode);

    return new MqlQueryBuildResult<>(
      itemQuery,
      countQuery);
  }

  private String buildOrderByClause()
  {
    var expr = "";

    final var termExprs = new LinkedList<String>();
    for(final var term : orderByTerms)
    {
      final var attrPath = term.getPath();
      final var path = String.format("%s", attrPath);
      final var direction = (term.isDescending() ? "DESC" : "ASC");
      var termExpr = String.format("%s %s", path, direction);

      final var nullOrder = Optional
        .ofNullable(term.getNullOrder())
        .orElse(NullOrder.DEFAULT);
      switch(nullOrder)
      {
      case DEFAULT:
        // Does nothing.
        break;
      case FIRST:
        termExpr = String.format("(CASE WHEN %s IS NULL THEN 0 ELSE 1 END) " + "ASC, ", path) + termExpr;
        break;
      case LAST:
        termExpr = String.format("(CASE WHEN %s IS NULL THEN 0 ELSE 1 END) " + "DESC, ", path) + termExpr;
        break;
      default:
        throw new MqlException(String.format("NullOrder %s is not supported", nullOrder.name()));
      }

      termExprs.add(termExpr);
    }

    if(!termExprs.isEmpty())
    {
      expr = termExprs.stream().collect(Collectors.joining(", "));
    }

    return (expr.isBlank() ? "" : "ORDER BY " + expr);
  }

  private Logger logger;

  private EntityManager em;

  private EntityType<? extends E> entityType;

  private Class<? extends E> entityJavaType;

  private String entityAlias;

  private ObjectMapper objectMapper;

  private Map<String, Object> mapFilter;

  private String filterJson;

  private List<Sort.Order> orderByTerms;

  private Integer limit;

  private Integer offset;
}
