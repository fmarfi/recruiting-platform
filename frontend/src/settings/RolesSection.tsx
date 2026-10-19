import { Table } from '@mantine/core';
import { useTranslation } from 'react-i18next';

// Fixed role matrix (NFR-05); read-only in Phase 1. Rows: capability key and which of the three roles have it.
const ROWS: [string, boolean[]][] = [
  ['c1', [true, true, true]],
  ['c2', [false, true, true]],
  ['c3', [false, true, true]],
  ['c4', [false, false, true]],
];
const ROLES = ['CONSULTANT', 'MANAGER', 'ADMIN'];

export function RolesSection() {
  const { t } = useTranslation();
  return (
    <Table withTableBorder>
      <Table.Thead>
        <Table.Tr>
          <Table.Th>{t('settings.roles.capability')}</Table.Th>
          {ROLES.map((r) => (
            <Table.Th key={r}>{t(`settings.role.${r}`)}</Table.Th>
          ))}
        </Table.Tr>
      </Table.Thead>
      <Table.Tbody>
        {ROWS.map(([k, has]) => (
          <Table.Tr key={k}>
            <Table.Td>{t(`settings.roles.${k}`)}</Table.Td>
            {has.map((h, i) => (
              <Table.Td key={ROLES[i]}>{h ? '✓' : '–'}</Table.Td>
            ))}
          </Table.Tr>
        ))}
      </Table.Tbody>
    </Table>
  );
}
