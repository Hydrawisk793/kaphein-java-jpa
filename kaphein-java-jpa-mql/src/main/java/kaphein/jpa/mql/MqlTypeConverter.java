package kaphein.jpa.mql;

import java.lang.reflect.Type;
import java.util.List;

interface MqlTypeConverter
{
  <T> T convertType(
    Object src,
    Type type
  );

  <E> List<E> convertElementType(
    List<?> list,
    Type elementType
  );
}
