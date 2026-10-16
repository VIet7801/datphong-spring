package com.vn.datphong.persistence;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.usertype.UserType;

import java.sql.*;

/** PostgreSQL daterange binding; avoids storing an inventory range as an ordinary VARCHAR. */
public class StayRangeType implements UserType<StayRange> {
    @Override public int getSqlType() { return Types.OTHER; }
    @Override public Class<StayRange> returnedClass() { return StayRange.class; }
    @Override public StayRange deepCopy(StayRange value) { return value; }
    @Override public boolean isMutable() { return false; }
    @Override public StayRange nullSafeGet(ResultSet rs, int position, WrapperOptions options) throws SQLException {
        return StayRange.parse(rs.getString(position));
    }
    @Override public void nullSafeSet(PreparedStatement statement, StayRange value, int index, WrapperOptions options) throws SQLException {
        if (value == null) statement.setNull(index, Types.OTHER);
        else statement.setObject(index, value.toString(), Types.OTHER);
    }
}
