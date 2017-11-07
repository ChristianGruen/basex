package org.basex.io.parse.csv;

import org.basex.build.csv.*;
import org.basex.query.*;
import org.basex.query.value.*;
import org.basex.query.value.array.*;
import org.basex.query.value.item.*;
import org.basex.query.value.map.*;

/**
 * This class converts CSV data to an XQuery representation.
 *
 * @author BaseX Team 2005-17, BSD License
 * @author Christian Gruen
 */
public final class CsvXQueryConverter extends CsvConverter {
  /** Field names. */
  public static final Str NAMES = Str.get("names");
  /** Records. */
  public static final Str RECORDS = Str.get("records");

  /** Rows. */
  private final ValueBuilder rows = new ValueBuilder();
  /** Current row. */
  private ArrayBuilder row;

  /**
   * Constructor.
   * @param opts CSV options
   */
  CsvXQueryConverter(final CsvParserOptions opts) {
    super(opts);
  }

  @Override
  protected void header(final byte[] string) {
    headers.add(string);
  }

  @Override
  protected void record() {
    if(row != null) rows.add(row.freeze());
    row = new ArrayBuilder();
  }

  @Override
  protected void entry(final byte[] value) throws QueryIOException {
    row.append(Str.get(value));
  }

  @Override
  protected Map finish() throws QueryIOException {
    if(row != null) rows.add(row.freeze());
    try {
      Map map = Map.EMPTY;
      if(!headers.isEmpty()) {
        final ArrayBuilder names = new ArrayBuilder();
        for(final byte[] header : headers) names.append(Str.get(header));
        map = map.put(NAMES, names.freeze(), null);
      }
      return map.put(RECORDS, rows.value(), null);
    } catch(final QueryException ex) {
      throw new QueryIOException(ex);
    }
  }
}