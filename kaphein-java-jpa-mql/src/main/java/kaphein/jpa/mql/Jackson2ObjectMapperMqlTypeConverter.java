package kaphein.jpa.mql;

import java.lang.reflect.Type;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import kaphein.jpa.core.AssertArg;

class Jackson2ObjectMapperMqlTypeConverter implements MqlTypeConverter
{
  public Jackson2ObjectMapperMqlTypeConverter()
  {
    this(Jackson2ObjectMapperUtils.getDefaultObjectMapper());
  }

  public Jackson2ObjectMapperMqlTypeConverter(ObjectMapper objectMapper)
  {
    this.objectMapper = AssertArg.isNotNull(objectMapper, "objectMapper");
  }

  @Override
  public <T> T convertType(
    Object src,
    Type type
  )
  {
    AssertArg.isNotNull(type, "type");

    final var typeFactory = objectMapper.getTypeFactory();

    return objectMapper.convertValue(src, typeFactory.constructType(type));
  }

  @Override
  public <E> List<E> convertElementType(
    List<?> list,
    Type elementType
  )
  {
    AssertArg.isNotNull(list, "list");
    AssertArg.isNotNull(elementType, "elementType");

    final var typeFactory = objectMapper.getTypeFactory();
    final var jacksonJavaType = typeFactory.constructCollectionLikeType(
      List.class,
      typeFactory.constructType(elementType));

    return objectMapper.convertValue(
      list,
      jacksonJavaType);
  }

  private final ObjectMapper objectMapper;
}
