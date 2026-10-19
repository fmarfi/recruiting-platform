import { Button, Menu } from '@mantine/core';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { NewAccountDialog } from '../../accounts/components/NewAccountDialog';
import { NewPersonDialog } from '../../network/list/NewPersonDialog';
import { QuickAdd } from '../../pipeline/quickadd/QuickAdd';

type Dialog = null | 'activity' | 'action' | 'account' | 'person';

/** Global "+ New" menu (UX-06): log activity, add action (pipeline quick add), new client, new person. */
export function NewMenu({ onChanged }: { onChanged: () => void }) {
  const { t } = useTranslation();
  const nav = useNavigate();
  const [dialog, setDialog] = useState<Dialog>(null);
  const close = () => setDialog(null);
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
          <Menu.Item onClick={() => setDialog('person')}>{t('network.newPerson')}</Menu.Item>
        </Menu.Dropdown>
      </Menu>
      <QuickAdd
        opened={dialog === 'activity' || dialog === 'action'}
        initialMode={dialog === 'action' ? 'action' : 'activity'}
        onClose={close}
        onSaved={onChanged}
      />
      <NewAccountDialog opened={dialog === 'account'} onClose={close} onCreated={(id) => nav(`/accounts/${id}`)} />
      <NewPersonDialog opened={dialog === 'person'} onClose={close} onCreated={onChanged} />
    </>
  );
}
