import { Pagination, type PaginationProps } from '@mantine/core';
import { useTranslation } from 'react-i18next';

/** Pagination whose arrow buttons and page buttons carry accessible names (TR/EN). */
export function AppPagination(props: PaginationProps) {
  const { t } = useTranslation();
  return (
    <Pagination
      withEdges={false}
      getControlProps={(control) => {
        const map = { first: 'first', last: 'last', next: 'next', previous: 'previous' } as const;
        return { 'aria-label': t(`shell.pagination.${map[control]}`) };
      }}
      getItemProps={(page) => ({ 'aria-label': t('shell.pagination.page', { n: page }) })}
      {...props}
    />
  );
}
