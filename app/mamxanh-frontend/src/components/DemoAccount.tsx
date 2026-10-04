import { createContext, useContext, useState, type ReactNode } from 'react';
import type { UserRole, ExpertApplicationStatus, ExpertApplication, DietTag } from '../types';
import { initialExpertApplications, currentUser } from '../data/mockData';

interface DemoAccountContextType {
  active: boolean;
  setActive: (active: boolean) => void;
  role: UserRole;
  setRole: (role: UserRole) => void;
  applicationStatus: ExpertApplicationStatus;
  setApplicationStatus: (status: ExpertApplicationStatus) => void;
  rejectionReason: string;
  setRejectionReason: (reason: string) => void;
  pendingAppId: string;
  applications: ExpertApplication[];
  submitApplication: (data: { experience: string; dietaryStyle: DietTag; sampleRecipe: string }) => { ok: boolean; message: string };
  reviewApplication: (id: string, action: 'APPROVE' | 'REJECT', reason?: string) => { ok: boolean; message: string };
}

const DemoAccountContext = createContext<DemoAccountContextType>({
  active: false,
  setActive: () => {},
  role: 'GUEST',
  setRole: () => {},
  applicationStatus: 'DRAFT',
  setApplicationStatus: () => {},
  rejectionReason: '',
  setRejectionReason: () => {},
  pendingAppId: '',
  applications: [],
  submitApplication: () => ({ ok: false, message: '' }),
  reviewApplication: () => ({ ok: false, message: '' }),
});

export function DemoAccountProvider({ children }: { children: ReactNode }) {
  const [active, setActiveState] = useState(false);
  const [role, setRole] = useState<UserRole>('GUEST');
  const [applicationStatus, setApplicationStatus] = useState<ExpertApplicationStatus>('DRAFT');
  const [rejectionReason, setRejectionReason] = useState<string>('');
  const [pendingAppId, setPendingAppId] = useState<string>('');
  const [applications, setApplications] = useState<ExpertApplication[]>(initialExpertApplications);

  const setActive = (newActive: boolean) => {
    setActiveState(newActive);
    if (!newActive) {
      setRole('GUEST');
    } else if (role === 'GUEST') {
      setRole('CUSTOMER');
    }
  };

  const submitApplication = (data: { experience: string; dietaryStyle: DietTag; sampleRecipe: string }) => {
    if (applicationStatus === 'PENDING') {
      return { ok: false, message: '409 Conflict: Bạn đang có đơn ở trạng thái PENDING, không thể gửi thêm.' };
    }
    const array = new Uint32Array(1);
    crypto.getRandomValues(array);
    const newId = `EX-${10000 + (array[0] % 90000)}`;
    const newApp: ExpertApplication = {
      id: newId,
      applicantId: currentUser.id,
      applicantName: currentUser.name,
      applicantEmail: 'lananh@example.com',
      experience: data.experience,
      dietaryStyle: data.dietaryStyle,
      sampleRecipe: data.sampleRecipe,
      status: 'PENDING',
      submittedAt: new Date().toLocaleString('vi-VN'),
    };
    setApplications((prev) => [newApp, ...prev]);
    setApplicationStatus('PENDING');
    setPendingAppId(newId);
    return { ok: true, message: 'Đã gửi đơn đăng ký Chuyên gia thành công.' };
  };

  const reviewApplication = (id: string, action: 'APPROVE' | 'REJECT', reason?: string) => {
    if (action === 'REJECT' && (!reason || reason.trim().length === 0)) {
      return { ok: false, message: 'Bắt buộc nhập lý do khi từ chối đơn đăng ký.' };
    }

    setApplications((prev) =>
        prev.map((app) => {
          if (app.id !== id) return app;
          return {
            ...app,
            status: action === 'APPROVE' ? 'APPROVED' : 'REJECTED',
            rejectionReason: action === 'REJECT' ? reason : undefined,
            reviewedAt: new Date().toLocaleString('vi-VN'),
          };
        })
    );

    // If reviewing current user's application
    const target = applications.find((a) => a.id === id);
    if (target && target.applicantId === currentUser.id) {
      if (action === 'APPROVE') {
        setApplicationStatus('APPROVED');
        setRole('EXPERT');
      } else {
        setApplicationStatus('REJECTED');
        setRejectionReason(reason || 'Chưa đáp ứng đủ tiêu chí theo yêu cầu.');
      }
    }

    return {
      ok: true,
      message: action === 'APPROVE' ? 'Đã phê duyệt đơn thành công.' : 'Đã từ chối đơn đăng ký.',
    };
  };

  return (
      <DemoAccountContext.Provider
          value={{
            active,
            setActive,
            role,
            setRole,
            applicationStatus,
            setApplicationStatus,
            rejectionReason,
            setRejectionReason,
            pendingAppId,
            applications,
            submitApplication,
            reviewApplication,
          }}
      >
        {children}
      </DemoAccountContext.Provider>
  );
}

export const useDemoAccount = () => useContext(DemoAccountContext);