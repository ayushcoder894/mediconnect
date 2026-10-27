import { Link } from "react-router-dom";
import { Compass } from "lucide-react";
import { Button } from "../../components/ui";

export default function NotFound() {
  return (
    <div className="flex min-h-[60vh] flex-col items-center justify-center px-4 text-center">
      <div className="rounded-2xl bg-brand-50 p-4 text-brand-600"><Compass className="h-8 w-8" /></div>
      <h1 className="mt-4 text-3xl font-extrabold text-slate-900">Page not found</h1>
      <p className="mt-2 text-slate-500">The page you are looking for has moved or does not exist.</p>
      <Link to="/" className="mt-6"><Button>Back to home</Button></Link>
    </div>
  );
}
