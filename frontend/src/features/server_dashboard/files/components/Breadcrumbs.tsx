interface BreadcrumbsProps {
  goToBreadcrumb: (index: number) => void;
  breadcrumbs: string[];
}

export function Breadcrumbs({ goToBreadcrumb, breadcrumbs }: BreadcrumbsProps) {
  return (
    <div>
      <a onClick={() => goToBreadcrumb(-1)}>root</a>
      {breadcrumbs.map((part, i) => (
        <span key={i}>
          <span className="breadcrumb-sep">/</span>
          <a className="breadcrumb-item" onClick={() => goToBreadcrumb(i)}>
            {part}
          </a>
        </span>
      ))}
    </div>
  );
}
