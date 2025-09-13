package kaphein.jpa.mql;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.helpers.NOPLogger;

import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.EntityType;
import kaphein.jpa.core.AssertArg;
import kaphein.jpa.core.Sort;

class MqlQueryEmitterConfig<E>
{
  public static abstract class MqlQueryEmitterConfigBuilder<
    E,
    C extends MqlQueryEmitterConfig<E>,
    B extends MqlQueryEmitterConfigBuilder<E, C, B>>
  {
    private Logger logger;

    private EntityManager entityManager;

    private EntityType<? extends E> entityType;

    private String entityAlias;

    private List<Sort.Order> orderByTerms;

    private Integer limit;

    private Integer offset;

    private MqlTypeConverter typeConverter;

    protected MqlQueryEmitterConfigBuilder()
    {
      logger(null);
      entityManager(null);
      entityType(null);
      entityAlias(null);
      orderByTerms(null);
      limit(null);
      offset(null);
      typeConverter(null);
    }

    public B logger(Logger logger)
    {
      this.logger = Optional
        .ofNullable(logger)
        .orElseGet(() -> NOPLogger.NOP_LOGGER);
      return self();
    }

    public B entityManager(EntityManager entityManager)
    {
      this.entityManager = entityManager;
      return self();
    }

    public B entityType(EntityType<? extends E> entityType)
    {
      this.entityType = entityType;
      return self();
    }

    public B entityAlias(String entityAlias)
    {
      this.entityAlias = entityAlias;
      return self();
    }

    public B orderByTerms(List<Sort.Order> orderByTerms)
    {
      this.orderByTerms = Optional
        .ofNullable(orderByTerms)
        .map((l) -> (List<Sort.Order>)new ArrayList<>(l))
        .orElseGet(ArrayList::new);
      return self();
    }

    public B orderByTerm(Sort.Order orderByTerm)
    {
      this.orderByTerms.add(orderByTerm);
      return self();
    }

    public B limit(Integer limit)
    {
      this.limit = Optional
        .ofNullable(limit)
        .orElse(Integer.MAX_VALUE);
      return self();
    }

    public B offset(Integer offset)
    {
      this.offset = Optional
        .ofNullable(offset)
        .orElse(0);
      return self();
    }

    public B typeConverter(MqlTypeConverter typeConverter)
    {
      this.typeConverter = typeConverter;
      return self();
    }

    public abstract B self();

    public abstract C build();
  }

  static class MqlQueryEmitterConfigBuilderImpl<E>
    extends MqlQueryEmitterConfigBuilder<E, MqlQueryEmitterConfig<E>, MqlQueryEmitterConfigBuilderImpl<E>>
  {
    @Override
    public MqlQueryEmitterConfigBuilderImpl<E> self()
    {
      return this;
    }

    @Override
    public MqlQueryEmitterConfig<E> build()
    {
      return new MqlQueryEmitterConfig<E>(this);
    }

    protected MqlQueryEmitterConfigBuilderImpl<E> $fillValuesFrom(final MqlQueryEmitterConfig<E> instance)
    {
      logger(instance.getLogger());
      entityManager(instance.getEntityManager());
      entityAlias(instance.getEntityAlias());
      orderByTerms(instance.getOrderByTerms());
      limit(instance.getLimit());
      offset(instance.getOffset());
      typeConverter(instance.getTypeConverter());
      return this.self();
    }
  }

  public static <E> MqlQueryEmitterConfigBuilder<E, ?, ?> builder()
  {
    return new MqlQueryEmitterConfigBuilderImpl<E>();
  }

  protected MqlQueryEmitterConfig(MqlQueryEmitterConfigBuilder<E, ?, ?> b)
  {
    this.logger = AssertArg.isNotNull(b.logger, "logger");
    this.entityManager = AssertArg.isNotNull(b.entityManager, "entityManager");
    this.entityType = AssertArg.isNotNull(b.entityType, "entityType");
    this.entityAlias = AssertArg.isNotBlank(b.entityAlias, "entityAlias");
    this.orderByTerms = AssertArg.isNotNull(b.orderByTerms, "orderByTerms");
    this.limit = AssertArg.isNotNull(b.limit, "limit");
    this.offset = AssertArg.isNotNull(b.offset, "offset");
    this.typeConverter = AssertArg.isNotNull(b.typeConverter, "typeConverter");
  }

  public Logger getLogger()
  {
    return logger;
  }

  public EntityManager getEntityManager()
  {
    return entityManager;
  }

  public EntityType<? extends E> getEntityType()
  {
    return entityType;
  }

  public String getEntityAlias()
  {
    return entityAlias;
  }

  public List<Sort.Order> getOrderByTerms()
  {
    return orderByTerms;
  }

  public Integer getLimit()
  {
    return limit;
  }

  public Integer getOffset()
  {
    return offset;
  }

  public MqlTypeConverter getTypeConverter()
  {
    return typeConverter;
  }

  public MqlQueryEmitterConfigBuilder<E, ?, ?> toBuilder()
  {
    final var b = new MqlQueryEmitterConfigBuilderImpl<E>();
    b.$fillValuesFrom(this);
    return b;
  }

  private final Logger logger;

  private final EntityManager entityManager;

  private final EntityType<? extends E> entityType;

  private final String entityAlias;

  private final List<Sort.Order> orderByTerms;

  private final Integer limit;

  private final Integer offset;

  private final MqlTypeConverter typeConverter;
}
