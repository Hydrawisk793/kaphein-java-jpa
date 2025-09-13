package kaphein.jpa.mql;

import java.util.AbstractMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import kaphein.jpa.core.AssertArg;

class Jackson2ObjectMapperMqlJsonExpressionDeserializer implements MqlJsonExpressonDeserializer
{
  public Jackson2ObjectMapperMqlJsonExpressionDeserializer()
  {
    this(Jackson2ObjectMapperUtils.getDefaultObjectMapper());
  }

  public Jackson2ObjectMapperMqlJsonExpressionDeserializer(
    ObjectMapper objectMapper
  )
  {
    this.objectMapper = Jackson2ObjectMapperUtils.configureObjectMapper(AssertArg.isNotNull(
      objectMapper,
      "objectMapper"));
  }

  @Override
  public Map<String, Object> deserializeAsMap(String jsonExpression)
  {
    Map<?, ?> deserialized = null;

    try
    {
      deserialized = (objectMapper.readValue(jsonExpression, Map.class));
    }
    catch(final JsonProcessingException jpe)
    {
      throw new IllegalArgumentException(jpe);
    }

    return deserialized
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

  private final ObjectMapper objectMapper;
}
