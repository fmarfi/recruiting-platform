<<<<<<< HEAD
import { Button, Kbd, Menu } from '@mantine/core';
import { openQuickLog } from '../quicklog';
=======
import { Button, Menu } from '@mantine/core';
import { IconPlus } from '@tabler/icons-react';
>>>>>>> feature/network-customer-tools
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { NewAccountDialog } from '../../accounts/components/NewAccountDialog';
import { NewPersonDialog } from '../../network/list/NewPersonDialog';
import { ICON_SIZE, ICON_STROKE } from '../components/objectIcons';
import { QuickAdd } from '../../pipeline/quickadd/QuickAdd';

type Dialog = null | 'action' | 'account' | 'person';

/** Global "New" menu (UX-06): log activity, add action (pipeline quick add), new client, new person. */
export function NewMenu({ onChanged }: { onChanged: () => void }) {
  const { t } = useTranslation();
  const nav = useNavigate();
  const [dialog, setDialog] = useState<Dialog>(null);
  const close = () => setDialog(null);
  return (
    <>
      <Menu position="bottom-end" withinPortal>
        <Menu.Target>
          <Button variant="default" size="sm" leftSection={<IconPlus size={ICON_SIZE} stroke={ICON_STROKE} aria-hidden />}>
            {t('shell.new')}
          </Button>
        </Menu.Target>
        <Menu.Dropdown>
          <Menu.Item onClick={() => openQuickLog()} rightSection={<Kbd size="xs">Alt L</Kbd>}>{t('accounts.logActivity')}</Menu.Item>
          <Menu.Item onClick={() => setDialog('action')}>{t('accounts.addAction')}</Menu.Item>
          <Menu.Item onClick={() => setDialog('account')}>{t('accounts.newAccount')}</Menu.Item>
          <Menu.Item onClick={() => setDialog('person')}>{t('network.newPerson')}</Menu.Item>
        </Menu.Dropdown>
      </Menu>
      <QuickAdd
        opened={dialog === 'action'}
        initialMode="action"
        onClose={close}
        onSaved={onChanged}
      />
      <NewAccountDialog opened={dialog === 'account'} onClose={close} onCreated={(id) => nav(`/clients/${id}`)} />
      <NewPersonDialog opened={dialog === 'person'} onClose={close} onCreated={onChanged} />
    </>
  );
}
