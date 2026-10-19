import { Button, Menu } from '@mantine/core';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { AddActionDialog } from '../../accounts/components/AddActionDialog';
import { LogActivityDialog } from '../../accounts/components/LogActivityDialog';
import { NewAccountDialog } from '../../accounts/components/NewAccountDialog';

/** Global "+ New" menu (UX-06): log activity, add action, new client from any screen. */
export function NewMenu({ onChanged }: { onChanged: () => void }) {
  const { t } = useTranslation();
  const nav = useNavigate();
  const [dialog, setDialog] = useState<null | 'activity' | 'action' | 'account'>(null);
  return (
    <>
      <Menu position="bottom-end" withinPortal>
        <Menu.Target>
          <Button size="sm">{t('shell.new')} ▾</Button>
        </Menu.Target>
        <Menu.Dropdown>
          <Menu.Item onClick={() => setDialog('activity')}>{t('accounts.logActivity')}</Menu.Item>
          <Menu.Item onClick={() => setDialog('action')}>{t('accounts.addAction')}</Menu.Item>
          <Menu.Item onClick={() => setDialog('account')}>{t('accounts.newAccount')}</Menu.Item>
        </Menu.Dropdown>
      </Menu>
      <LogActivityDialog opened={dialog === 'activity'} onClose={() => setDialog(null)} onDone={onChanged} />
      <AddActionDialog opened={dialog === 'action'} onClose={() => setDialog(null)} onDone={onChanged} />
      <NewAccountDialog opened={dialog === 'account'} onClose={() => setDialog(null)} onCreated={(id) => nav(`/accounts/${id}`)} />
    </>
  );
}
