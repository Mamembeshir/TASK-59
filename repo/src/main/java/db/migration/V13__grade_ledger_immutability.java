package db.migration;

import java.sql.Statement;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V13__grade_ledger_immutability extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        try (Statement stmt = context.getConnection().createStatement()) {
            stmt.execute(
                "CREATE TRIGGER trg_grade_ledger_no_update "
                    + "BEFORE UPDATE ON grade_ledger_entries "
                    + "FOR EACH ROW "
                    + "BEGIN "
                    + "SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Grade ledger entries are immutable and cannot be updated'; "
                    + "END"
            );
            stmt.execute(
                "CREATE TRIGGER trg_grade_ledger_no_delete "
                    + "BEFORE DELETE ON grade_ledger_entries "
                    + "FOR EACH ROW "
                    + "BEGIN "
                    + "SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Grade ledger entries are immutable and cannot be deleted'; "
                    + "END"
            );
        }
    }
}
