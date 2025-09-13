package kaphein.jpa.mql;

import java.util.AbstractMap;
import java.util.LinkedHashMap;
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
import kaphein.jpa.core.MetamodelUtils;
import kaphein.jpa.core.Sort;
import kaphein.jpa.core.StringUtils;

public class MqlQueryBuilder<E>
{
  public MqlQueryBuilder()
  {
    logger(null);
    queryEmitterConfigBuilder = MqlQueryEmitterConfig.<E>builder();
  }

  public MqlQueryBuilder(MqlQueryBuilder<E> src)
  {
    AssertArg.isNotNull(src, "src");

    logger(src.logger);
    entityManager = src.entityManager;
    entityType = src.entityType;
    entityJavaType = src.entityJavaType;
    entityAlias = src.entityAlias;
    jsonExprDeser = src.jsonExprDeser;
    objectMapper = src.objectMapper;
    mapFilter = src.mapFilter;
    filterJson = src.filterJson;
    queryEmitterConfigBuilder = src.queryEmitterConfigBuilder;
    queryEmitterConfig = src.queryEmitterConfig;
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
    this.entityManager = entityManager;

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

  public MqlQueryBuilder<E> jsonExpressionDeserializer(MqlJsonExpressonDeserializer jsonExprDeser)
  {
    this.jsonExprDeser = jsonExprDeser;

    return this;
  }

  /**
   * @deprecated Use {@link MqlQueryBuilder#jsonExpressionDeserializer} instead.
   */
  @Deprecated
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
    queryEmitterConfigBuilder.orderByTerms(orderByTerms);

    return this;
  }

  public MqlQueryBuilder<E> limit(Integer limit)
  {
    queryEmitterConfigBuilder.limit(limit);

    return this;
  }

  public MqlQueryBuilder<E> offset(Integer offset)
  {
    queryEmitterConfigBuilder.offset(offset);

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
    if(null == jsonExprDeser)
    {
      jsonExprDeser = (null == objectMapper
        ? new Jackson2ObjectMapperMqlJsonExpressionDeserializer()
        : new Jackson2ObjectMapperMqlJsonExpressionDeserializer(objectMapper));
    }

    ensureMapFilter();

    ensureEntityType();

    if(StringUtils.isBlank(entityAlias))
    {
      entityAlias = (String)mapFilter.get("$alias");
      if(StringUtils.isBlank(entityAlias))
      {
        throw new IllegalArgumentException("$alias is missing.");
      }
    }

    final var typeConverter = (null == objectMapper
      ? new Jackson2ObjectMapperMqlTypeConverter()
      : new Jackson2ObjectMapperMqlTypeConverter(objectMapper));

    queryEmitterConfig = queryEmitterConfigBuilder
      .entityManager(entityManager)
      .entityType(entityType)
      .entityAlias(entityAlias)
      .typeConverter(typeConverter)
      .build();
  }

  private void ensureEntityType()
  {
    if(null == entityType)
    {
      if(null == entityJavaType)
      {
        throw new IllegalArgumentException("Either entityType or entityJavaType must be set.");
      }

      entityType = MetamodelUtils.findEntityTypeByJavaTypeOrThrow(
        entityManager.getMetamodel(),
        entityJavaType);
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
      mapFilter,
      entityType.getName());
    final var parseResult = StreamSupport
      .stream(
        Spliterators.spliteratorUnknownSize(parser.iterator(), Spliterator.ORDERED),
        false)
      .filter(MqlExpressionParseResult::isCompleted)
      .findFirst()
      .orElseThrow();

    final var emitter = new MqlQueryEmitter<>(queryEmitterConfig);
    final var result = emitter.emit(parseResult);

    return result;
  }

  private Logger logger;

  private EntityManager entityManager;

  private EntityType<? extends E> entityType;

  private Class<? extends E> entityJavaType;

  private String entityAlias;

  private MqlJsonExpressonDeserializer jsonExprDeser;

  private ObjectMapper objectMapper;

  private Map<String, Object> mapFilter;

  private String filterJson;

  private MqlQueryEmitterConfig.MqlQueryEmitterConfigBuilder<E, ?, ?> queryEmitterConfigBuilder;

  private MqlQueryEmitterConfig<E> queryEmitterConfig;
}
